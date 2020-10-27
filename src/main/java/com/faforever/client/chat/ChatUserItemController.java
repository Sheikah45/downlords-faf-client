package com.faforever.client.chat;

import com.faforever.client.clan.Clan;
import com.faforever.client.clan.ClanTooltipController;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.PopupWindow;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Optional;

import static com.faforever.client.chat.ChatColorMode.CUSTOM;
import static com.faforever.client.player.SocialStatus.SELF;
import static com.faforever.client.util.RatingUtil.getGlobalRating;
import static com.faforever.client.util.RatingUtil.getLeaderboardRating;
import static java.util.Locale.US;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
// TODO null safety for "player"
public class ChatUserItemController implements Controller<Node> {

  private static final PseudoClass COMPACT = PseudoClass.getPseudoClass("compact");

  private static volatile Map<PlayerStatus, Image> playerStatusIcons;

  private final PreferencesService preferencesService;
  private final I18n i18n;
  private final UiService uiService;
  private final EventBus eventBus;
  private final PlayerService playerService;
  private final PlatformService platformService;
  private final TimeService timeService;

  private final InvalidationListener colorChangeListener;
  private final InvalidationListener formatChangeListener;
  private final InvalidationListener colorPerUserInvalidationListener;
  private final ChangeListener<Image> avatarChangeListener;
  private final ChangeListener<Clan> clanChangeListener;
  private final ChangeListener<Image> countryChangeListener;
  private final ChangeListener<PlayerStatus> statusChangeListener;
  private final InvalidationListener userActivityListener;
  private final ChangeListener<String> usernameChangeListener;
  private final WeakChangeListener<String> weakUsernameChangeListener;
  private final WeakInvalidationListener weakColorInvalidationListener;
  private final WeakInvalidationListener weakFormatInvalidationListener;
  private final WeakInvalidationListener weakUserActivityListener;
  private final WeakChangeListener<Image> weakAvatarChangeListener;
  private final WeakChangeListener<Clan> weakClanChangeListener;
  private final WeakChangeListener<Image> weakCountryChangeListener;
  private final WeakChangeListener<PlayerStatus> weakStatusChangeListener;
  private final WeakInvalidationListener weakColorPerUserInvalidationListener;

  public ImageView playerStatusIndicator;

  public Pane chatUserItemRoot;
  public ImageView countryImageView;
  public ImageView avatarImageView;
  public Label usernameLabel;
  public MenuButton clanMenu;
  public ImageView playerMapImage;

  private ChatChannelUser chatUser;
  @VisibleForTesting
  protected Tooltip countryTooltip;
  @VisibleForTesting
  protected Tooltip avatarTooltip;
  @VisibleForTesting
  protected Tooltip userTooltip;
  private Clan clan;
  private ClanTooltipController clanTooltipController;
  private Tooltip clanTooltip;

  // TODO reduce dependencies, rely on eventBus instead
  public ChatUserItemController(PreferencesService preferencesService,
                                I18n i18n, UiService uiService, EventBus eventBus, PlayerService playerService,
                                PlatformService platformService, TimeService timeService) {
    this.platformService = platformService;
    this.preferencesService = preferencesService;
    this.playerService = playerService;
    this.i18n = i18n;
    this.uiService = uiService;
    this.eventBus = eventBus;
    this.timeService = timeService;
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    colorPerUserInvalidationListener = change -> {
      if (chatUser != null) {
        updateColor();
      }
    };

    colorChangeListener = observable -> updateColor();
    formatChangeListener = observable -> updateFormat();
    weakColorInvalidationListener = new WeakInvalidationListener(colorChangeListener);
    weakFormatInvalidationListener = new WeakInvalidationListener(formatChangeListener);
    weakColorPerUserInvalidationListener = new WeakInvalidationListener(colorPerUserInvalidationListener);

    JavaFxUtil.addListener(chatPrefs.chatColorModeProperty(), weakColorInvalidationListener);
    JavaFxUtil.addListener(chatPrefs.chatFormatProperty(), weakFormatInvalidationListener);

    userActivityListener = (observable) -> JavaFxUtil.runLater(this::onUserActivity);
    avatarChangeListener = (observable, oldValue, newValue) -> JavaFxUtil.runLater(this::setAvatar);
    clanChangeListener = (observable, oldValue, newValue) -> JavaFxUtil.runLater(this::setClan);
    countryChangeListener = (observable, oldValue, newValue) -> JavaFxUtil.runLater(this::setCountry);
    statusChangeListener = (observable, oldValue, newValue) -> JavaFxUtil.runLater(this::updateGameStatus);

    weakUserActivityListener = new WeakInvalidationListener(userActivityListener);
    weakAvatarChangeListener = new WeakChangeListener<>(avatarChangeListener);
    weakClanChangeListener = new WeakChangeListener<>(clanChangeListener);
    weakCountryChangeListener = new WeakChangeListener<>(countryChangeListener);
    weakStatusChangeListener = new WeakChangeListener<>(statusChangeListener);

    usernameChangeListener = (observable, oldValue, newValue) -> {
      updateNameLabelTooltip();
      if (this.chatUser == null) {
        usernameLabel.setText("");
      } else {
        usernameLabel.setText(this.chatUser.getUsername());
      }
    };
    weakUsernameChangeListener = new WeakChangeListener<>(usernameChangeListener);
  }

  private void initClanTooltip() {
    clanTooltipController = uiService.loadFxml("theme/chat/clan_tooltip.fxml");

    clanTooltip = new Tooltip();
    clanTooltip.setGraphic(clanTooltipController.getRoot());
    Tooltip.install(clanMenu, clanTooltip);
  }

  private void updateFormat() {
    ChatFormat chatFormat = preferencesService.getPreferences().getChat().getChatFormat();
    if (chatFormat == ChatFormat.COMPACT) {
      JavaFxUtil.removeListener(preferencesService.getPreferences().getChat().getUserToColor(), weakColorPerUserInvalidationListener);
      JavaFxUtil.addListener(preferencesService.getPreferences().getChat().getUserToColor(), weakColorPerUserInvalidationListener);
    }
    getRoot().pseudoClassStateChanged(
        COMPACT,
        chatFormat == ChatFormat.COMPACT
    );
  }

  public void initialize() {
    chatUserItemRoot.setUserData(this);
    countryImageView.managedProperty().bind(countryImageView.visibleProperty());
    countryImageView.setVisible(false);
    clanMenu.managedProperty().bind(clanMenu.visibleProperty());
    clanMenu.setVisible(false);

    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    weakColorInvalidationListener.invalidated(chatPrefs.chatColorModeProperty());
    weakFormatInvalidationListener.invalidated(chatPrefs.chatFormatProperty());

    playerStatusIndicator.managedProperty().bind(playerStatusIndicator.visibleProperty());
    playerMapImage.managedProperty().bind(playerMapImage.visibleProperty());

    updateFormat();
    initClanTooltip();
  }

  private WeakReference<ChatUserContextMenuController> contextMenuController = null;

  public void onContextMenuRequested(ContextMenuEvent event) {
    if (contextMenuController != null) {
      ChatUserContextMenuController controller = contextMenuController.get();
      if (controller != null) {
        controller.getContextMenu().show(chatUserItemRoot.getScene().getWindow(), event.getScreenX(), event.getScreenY());
        return;
      }
    }

    ChatUserContextMenuController controller = uiService.loadFxml("theme/chat/chat_user_context_menu.fxml");
    controller.setChatUser(chatUser);
    controller.getContextMenu().show(chatUserItemRoot.getScene().getWindow(), event.getScreenX(), event.getScreenY());

    contextMenuController = new WeakReference<>(controller);
  }

  public void onItemClicked(MouseEvent mouseEvent) {
    if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
      eventBus.post(new InitiatePrivateChatEvent(chatUser.getUsername()));
    }
  }

  private void updateColor() {
    if (chatUser == null) {
      return;
    }
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    chatUser.getPlayer().ifPresent(player -> {
      if (player.getSocialStatus() == SELF) {
        usernameLabel.getStyleClass().add(SELF.getCssClass());
        clanMenu.getStyleClass().add(SELF.getCssClass());
      }
    });

    Color color = null;
    String lowerUsername = chatUser.getUsername().toLowerCase(US);

    if (chatPrefs.getChatColorMode() == CUSTOM) {
      if (chatPrefs.getUserToColor().containsKey(lowerUsername)) {
        color = chatPrefs.getUserToColor().get(lowerUsername);
      }
    } else if (chatPrefs.getChatColorMode() == ChatColorMode.RANDOM) {
      color = ColorGeneratorUtil.generateRandomColor(chatUser.getUsername().hashCode());
    }

    chatUser.setColor(color);
    assignColor(color);
  }

  private void assignColor(Color color) {
    if (color != null) {
      usernameLabel.setStyle(String.format("-fx-text-fill: %s", JavaFxUtil.toRgbCode(color)));
      clanMenu.setStyle(String.format("-fx-text-fill: %s", JavaFxUtil.toRgbCode(color)));
    } else {
      usernameLabel.setStyle("");
      clanMenu.setStyle("");
    }
  }

  private void setAvatar() {
    updateAvatarTooltip();
    Optional<Image> avatarImage = chatUser.getAvatar();
    if (avatarImage.isPresent()) {
      avatarImageView.setImage(avatarImage.get());
      avatarImageView.setVisible(true);
    } else {
      avatarImageView.setVisible(false);
    }
  }

  public Pane getRoot() {
    return chatUserItemRoot;
  }

  public ChatChannelUser getChatUser() {
    return chatUser;
  }

  public void setChatUser(@Nullable ChatChannelUser chatUser) {
    if (this.chatUser == chatUser) {
      return;
    }

    if (this.chatUser != null) {
      removeListeners(this.chatUser);
    }

    this.chatUser = chatUser;
    if (this.chatUser != null) {
      addListeners(this.chatUser);
    }
  }

  private void updateCountryTooltip() {
    Optional.ofNullable(countryTooltip).ifPresent(imageView -> Tooltip.uninstall(countryImageView, countryTooltip));

    chatUser.getCountryName().ifPresent(countryName -> {
      countryTooltip = new Tooltip(countryName);
      countryTooltip.showDelayProperty().set(Duration.millis(250));
      Tooltip.install(countryImageView, countryTooltip);
    });
  }

  private void setClan() {
    clan = chatUser.getClan().orElse(null);
    if (clan == null) {
      clanMenu.setVisible(false);
    } else {
      clanMenu.setText(String.format("[%s]", clan.getTag()));
      clanMenu.setVisible(true);
    }
  }

  private void updateNameLabelTooltip() {
    Optional.ofNullable(usernameLabel.getTooltip()).ifPresent(tooltip -> usernameLabel.setTooltip(null));

    if (chatUser == null || !chatUser.getPlayer().isPresent()) {
      return;
    }

    chatUser.getPlayer().ifPresent(player -> {
      userTooltip = new Tooltip();
      usernameLabel.setTooltip(userTooltip);
      updateNameLabelText(player);
    });
  }

  private void updateNameLabelText(Player player) {
    userTooltip.setText(String.format("%s\n%s",
        i18n.get("userInfo.ratingFormat", getGlobalRating(player), getLeaderboardRating(player)),
        i18n.get("userInfo.idleTimeFormat", timeService.timeAgo(player.getIdleSince()))));
  }

  private void addListeners(@NotNull ChatChannelUser chatUser) {
    JavaFxUtil.addListener(chatUser.usernameProperty(), weakUsernameChangeListener);
    JavaFxUtil.addListener(chatUser.colorProperty(), weakColorInvalidationListener);
    JavaFxUtil.addListener(chatUser.avatarProperty(), weakAvatarChangeListener);
    JavaFxUtil.addListener(chatUser.clanProperty(), weakClanChangeListener);
    JavaFxUtil.addListener(chatUser.statusProperty(), weakUserActivityListener);
    JavaFxUtil.addListener(chatUser.countryFlagProperty(), weakCountryChangeListener);
    JavaFxUtil.addListener(chatUser.statusProperty(), weakStatusChangeListener);

    weakUsernameChangeListener.changed(chatUser.usernameProperty(), null, null);
    weakColorInvalidationListener.invalidated(chatUser.colorProperty());
    weakAvatarChangeListener.changed(chatUser.avatarProperty(), null, null);
    weakClanChangeListener.changed(chatUser.clanProperty(), null, null);
    weakCountryChangeListener.changed(chatUser.countryFlagProperty(), null, null);
    weakStatusChangeListener.changed(chatUser.statusProperty(), null, null);
    weakUserActivityListener.invalidated(chatUser.statusProperty());
  }

  private void removeListeners(@NotNull ChatChannelUser chatUser) {
    JavaFxUtil.removeListener(chatUser.usernameProperty(), weakUsernameChangeListener);
    JavaFxUtil.removeListener(chatUser.colorProperty(), weakColorInvalidationListener);
    JavaFxUtil.removeListener(chatUser.avatarProperty(), weakAvatarChangeListener);
    JavaFxUtil.removeListener(chatUser.clanProperty(), weakClanChangeListener);
    JavaFxUtil.removeListener(chatUser.statusProperty(), weakUserActivityListener);
    JavaFxUtil.removeListener(chatUser.countryFlagProperty(), weakCountryChangeListener);
    JavaFxUtil.removeListener(chatUser.statusProperty(), weakStatusChangeListener);

    weakUsernameChangeListener.changed(chatUser.usernameProperty(), null, null);
    weakAvatarChangeListener.changed(chatUser.avatarProperty(), null, null);
    weakClanChangeListener.changed(chatUser.clanProperty(), null, null);
    weakUserActivityListener.invalidated(chatUser.statusProperty());
    weakStatusChangeListener.changed(chatUser.statusProperty(), null, null);
    weakCountryChangeListener.changed(chatUser.countryFlagProperty(), null, null);
  }

  private void setCountry() {
    Optional<Image> countryFlag = chatUser.getCountryFlag();
    if (countryFlag.isPresent()) {
      countryImageView.setImage(countryFlag.get());
      countryImageView.setVisible(true);
      updateCountryTooltip();
    } else {
      countryImageView.setVisible(false);
    }
  }

  public void setVisible(boolean visible) {
    chatUserItemRoot.setVisible(visible);
    chatUserItemRoot.setManaged(visible);
  }

  private void onUserActivity() {
    // TODO only until server-side support
    updateGameStatus();
    if (chatUser.getPlayer().isPresent() && userTooltip != null) {
      updateNameLabelText(chatUser.getPlayer().get());
    }
  }

  public void updateAvatarTooltip() {
    Optional.ofNullable(avatarTooltip).ifPresent(tooltip -> Tooltip.uninstall(avatarImageView, tooltip));

    chatUser.getPlayer().ifPresent(player -> {
      avatarTooltip = new Tooltip(player.getAvatarTooltip());
      avatarTooltip.textProperty().bind(player.avatarTooltipProperty());
      avatarTooltip.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);

      Tooltip.install(avatarImageView, avatarTooltip);
    });
  }

  private void updateGameStatus() {
    if (chatUser == null) {
      return;
    }
    Optional<Player> playerOptional = chatUser.getPlayer();
    if (playerOptional.isEmpty()) {
      playerStatusIndicator.setVisible(false);
      playerMapImage.setVisible(false);
      return;
    }

    Player player = playerOptional.get();

    if (player.getStatus() == PlayerStatus.IDLE) {
      playerStatusIndicator.setVisible(false);
      playerMapImage.setVisible(false);
    } else {
      playerStatusIndicator.setVisible(true);
      playerStatusIndicator.setImage(chatUser.getStatusImage().orElse(null));
      playerMapImage.setVisible(true);
      playerMapImage.setImage(chatUser.getMapImage().orElse(null));
    }
  }

  public void onMouseEnteredUserNameLabel() {
    chatUser.getPlayer().ifPresent(this::updateNameLabelText);
  }

  /**
   * Memory analysis suggest this menu uses tons of memory while it stays unclear why exactly (something java fx
   * internal). We just load the menu on click. Also we destroy it over and over again.
   */
  public void onClanMenuRequested() {
    clanMenu.getItems().clear();
    if (clan == null) {
      return;
    }

    Player currentPlayer = playerService.getCurrentPlayer()
        .orElseThrow(() -> new IllegalStateException("Player has to be set"));

    if (currentPlayer.getId() != clan.getLeader().getId()
        && playerService.isOnline(clan.getLeader().getId())) {
      MenuItem messageLeaderItem = new MenuItem(i18n.get("clan.messageLeader"));
      messageLeaderItem.setOnAction(event -> eventBus.post(new InitiatePrivateChatEvent(clan.getLeader().getUsername())));
      clanMenu.getItems().add(messageLeaderItem);
    }

    MenuItem visitClanPageAction = new MenuItem(i18n.get("clan.visitPage"));
    visitClanPageAction.setOnAction(event -> platformService.showDocument(clan.getWebsiteUrl()));
    clanMenu.getItems().add(visitClanPageAction);

    //reload to load newly set UI
    clanMenu.hide();
    clanMenu.show();
  }

  /**
   * Because of a bug in java fx tooltip when controls are updated without the tooltip showing the ui is leaked, so we
   * only update when the user hover over the tooltip. This will reduce that to a minimum.
   */
  public void updateClanData() {
    if (clan == null) {
      return;
    }
    clanTooltipController.setClan(clan);
    clanTooltip.setMaxHeight(clanTooltipController.getRoot().getHeight());
  }
}
