package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.ClientMessageType;
import com.faforever.client.legacy.domain.GameTypeInfo;
import com.faforever.client.legacy.domain.InitSessionMessage;
import com.faforever.client.legacy.domain.LoginMessage;
import com.faforever.client.legacy.domain.ServerMessage;
import com.faforever.client.legacy.domain.ServerMessageType;
import com.faforever.client.legacy.domain.SessionInfo;
import com.faforever.client.legacy.gson.ClientMessageTypeTypeAdapter;
import com.faforever.client.legacy.gson.ServerMessageTypeTypeAdapter;
import com.faforever.client.legacy.io.QDataInputStream;
import com.faforever.client.legacy.writer.ServerWriter;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.util.Callback;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.testfx.util.WaitForAsyncUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LobbyServerAccessorImplTest extends AbstractPlainJavaFxTest {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final long TIMEOUT = 500000;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
  private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();
  private static final Gson gson = new GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .registerTypeAdapter(ClientMessageType.class, new ClientMessageTypeTypeAdapter())
      .registerTypeAdapter(ServerMessageType.class, new ServerMessageTypeTypeAdapter())
      .create();

  @Rule
  public TemporaryFolder faDirectory = new TemporaryFolder();

  @Mock
  PreferencesService preferencesService;
  @Mock
  Preferences preferences;
  @Mock
  Environment environment;
  @Mock
  UidService uidService;

  private LobbyServerAccessorImpl instance;
  private LoginPrefs loginPrefs;
  private ServerSocket fafLobbyServerSocket;
  private Socket localToServerSocket;
  private ServerWriter serverToClientWriter;
  private boolean stopped;
  private BlockingQueue<String> messagesReceivedByFafServer;
  private CountDownLatch serverToClientReadyLatch;

  @Before
  public void setUp() throws Exception {
    serverToClientReadyLatch = new CountDownLatch(1);
    messagesReceivedByFafServer = new ArrayBlockingQueue<>(10);

    startFakeFafLobbyServer();

    instance = new LobbyServerAccessorImpl();
    instance.preferencesService = preferencesService;
    instance.environment = environment;
    instance.uidService = uidService;

    loginPrefs = new LoginPrefs();
    loginPrefs.setUsername("junit");
    loginPrefs.setPassword("password");

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferencesService.getFafDataDirectory()).thenReturn(faDirectory.getRoot().toPath());
    when(preferences.getLogin()).thenReturn(loginPrefs);
    when(environment.getProperty("lobby.host")).thenReturn(LOOPBACK_ADDRESS.getHostAddress());
    when(environment.getProperty("lobby.port", int.class)).thenReturn(fafLobbyServerSocket.getLocalPort());
    when(uidService.generate(any(), any())).thenReturn("encrypteduidstring");

    preferencesService.getPreferences().getLogin();
  }

  private void startFakeFafLobbyServer() throws IOException {
    fafLobbyServerSocket = new ServerSocket(0);
    logger.info("Fake server listening on " + fafLobbyServerSocket.getLocalPort());

    WaitForAsyncUtils.async(() -> {

      try (Socket socket = fafLobbyServerSocket.accept()) {
        localToServerSocket = socket;
        QDataInputStream qDataInputStream = new QDataInputStream(new DataInputStream(socket.getInputStream()));
        serverToClientWriter = new ServerWriter(socket.getOutputStream());
        serverToClientWriter.registerMessageSerializer(new ServerMessageSerializer(), ServerMessage.class);

        serverToClientReadyLatch.countDown();

        while (!stopped) {
          int blockSize = qDataInputStream.readInt();
          String json = qDataInputStream.readQString();

          if (blockSize > json.length() * 2) {
            String username = qDataInputStream.readQString();
            String sessionId = qDataInputStream.readQString();
          }

          messagesReceivedByFafServer.add(json);
        }
      } catch (IOException e) {
        System.out.println("Closing fake FAF lobby server: " + e.getMessage());
        throw new RuntimeException(e);
      }
    });
  }

  @After
  public void tearDown() {
    IOUtils.closeQuietly(fafLobbyServerSocket);
    IOUtils.closeQuietly(localToServerSocket);
  }

  @Test
  public void testConnectAndLogInInBackground() throws Exception {
    CompletableFuture<SessionInfo> sessionInfoFuture = new CompletableFuture<>();
    @SuppressWarnings("unchecked")
    Callback<SessionInfo> callback = mock(Callback.class);
    doAnswer(invocation -> {
      sessionInfoFuture.complete(invocation.getArgumentAt(0, SessionInfo.class));
      return null;
    }).when(callback).success(any());

    int playerUid = 123;
    String sessionId = "456";
    String email = "test@example.com";

    instance.connectAndLogInInBackground(callback);

    String json = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    InitSessionMessage initSessionMessage = gson.fromJson(json, InitSessionMessage.class);

    assertThat(initSessionMessage.getCommand(), is(ClientMessageType.ASK_SESSION));
    assertThat(initSessionMessage.getAction(), nullValue());

    SessionInfo sessionInfo = new SessionInfo();
    sessionInfo.setId(playerUid);
    sessionInfo.setSession(sessionId);

    sendFromServer(sessionInfo);

    json = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    LoginMessage loginMessage = gson.fromJson(json, LoginMessage.class);

    assertThat(loginMessage.getCommand(), is(ClientMessageType.LOGIN));
    assertThat(loginMessage.getAction(), nullValue());
    assertThat(loginMessage.getLogin(), is("junit"));
    assertThat(loginMessage.getPassword(), is("password"));
    assertThat(loginMessage.getSession(), is(sessionId));
    assertThat(loginMessage.getUniqueId(), is("encrypteduidstring"));
    assertThat(loginMessage.getVersion(), is(0));
    assertThat(loginMessage.getUserAgent(), is("downlords-faf-client"));

    sessionInfo = new SessionInfo();
    sessionInfo.setId(playerUid);
    sessionInfo.setEmail(email);

    sendFromServer(sessionInfo);

    SessionInfo result = sessionInfoFuture.get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(result.getServerMessageType(), is(ServerMessageType.WELCOME));
    assertThat(result.getId(), is(playerUid));
    assertThat(result.getSession(), is(sessionId));
    assertThat(result.getEmail(), is(email));
  }

  /**
   * Writes the specified message to the client as if it was sent by the FAF server.
   */
  private void sendFromServer(ServerMessage serverMessage) throws InterruptedException {
    serverToClientReadyLatch.await();
    serverToClientWriter.write(serverMessage);
  }

  @Test
  public void testAddOnGameTypeInfoListener() throws Exception {
    connectAndLogIn();

    CompletableFuture<GameTypeInfo> gameTypeInfoFuture = new CompletableFuture<>();
    @SuppressWarnings("unchecked")
    OnGameTypeInfoListener listener = mock(OnGameTypeInfoListener.class);
    doAnswer(invocation -> {
      gameTypeInfoFuture.complete(invocation.getArgumentAt(0, GameTypeInfo.class));
      return null;
    }).when(listener).onGameTypeInfo(any());

    instance.addOnGameTypeInfoListener(listener);

    String name = "test";
    String fullname = "Test game type";
    String description = "Game type description";
    String icon = "what";
    Boolean[] options = new Boolean[]{TRUE, FALSE, TRUE};

    GameTypeInfo gameTypeInfo = new GameTypeInfo();
    gameTypeInfo.setName(name);
    gameTypeInfo.setFullname(fullname);
    gameTypeInfo.setDesc(description);
    gameTypeInfo.setIcon(icon);
    gameTypeInfo.setOptions(options);

    sendFromServer(gameTypeInfo);

    GameTypeInfo result = gameTypeInfoFuture.get(TIMEOUT, TIMEOUT_UNIT);
    assertThat(result.getName(), is(name));
    assertThat(result.getFullname(), is(fullname));
    assertThat(result.getServerMessageType(), is(ServerMessageType.GAME_TYPE_INFO));
    assertThat(result.getDesc(), is(description));
    assertThat(result.getIcon(), is(icon));
    assertThat(result.getOptions(), is(options));
  }

  private void connectAndLogIn() throws InterruptedException {
    CountDownLatch connectedLatch = new CountDownLatch(1);
    @SuppressWarnings("unchecked")
    Callback<SessionInfo> connectionCallback = mock(Callback.class);
    doAnswer(invocation -> {
      connectedLatch.countDown();
      return null;
    }).when(connectionCallback).success(any());


    instance.connectAndLogInInBackground(connectionCallback);

    assertNotNull(messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT));

    SessionInfo sessionInfo = new SessionInfo();
    sessionInfo.setSession("5678");

    sendFromServer(sessionInfo);

    assertNotNull(messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT));

    sessionInfo = new SessionInfo();
    sessionInfo.setEmail("junit@example.com");

    sendFromServer(sessionInfo);

    assertTrue(connectedLatch.await(TIMEOUT, TIMEOUT_UNIT));
  }
}