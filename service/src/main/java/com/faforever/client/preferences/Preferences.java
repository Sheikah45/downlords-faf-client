package com.faforever.client.preferences;

import com.faforever.client.game.KnownFeaturedMod;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.control.TableColumn.SortType;
import javafx.util.Pair;
import lombok.Getter;

import java.net.HttpCookie;
import java.net.URI;
import java.util.ArrayList;

import static javafx.collections.FXCollections.observableArrayList;

public class Preferences {

  public static final String DEFAULT_THEME_NAME = "default";

  private final WindowPrefs mainWindow;
  private final ForgedAlliancePrefs forgedAlliance;
  private final LoginPrefs login;
  private final ChatPrefs chat;
  private final NotificationsPrefs notification;
  private final StringProperty themeName;
  private final StringProperty lastGameType;
  private final LocalizationPrefs localization;
  private final StringProperty lastGameTitle;
  private final StringProperty lastMap;
  private final BooleanProperty rememberLastTab;
  private final BooleanProperty showPasswordProtectedGames;
  private final BooleanProperty showModdedGames;
  private final ListProperty<String> ignoredNotifications;
  private final IntegerProperty lastGameMinRank;
  private final IntegerProperty lastGameMaxRank;
  private final StringProperty gamesViewMode;
  private final Ladder1v1Prefs ladder1v1;
  private final NewsPrefs news;
  private final DeveloperPrefs developer;
  private final VaultPrefs vaultPrefs;
  private final ListProperty<Pair<String, SortType>> gameListSorting;
  private final ObjectProperty<TilesSortingOrder> gameTileSortingOrder;
  private final ObjectProperty<UnitDataBaseType> unitDataBaseType;
  private final MapProperty<URI, ArrayList<HttpCookie>> storedCookies;
  private final BooleanProperty lastGameOnlyFriends;

  public Preferences() {
    gameTileSortingOrder = new SimpleObjectProperty<>(TilesSortingOrder.PLAYER_DES);
    chat = new ChatPrefs();
    login = new LoginPrefs();

    localization = new LocalizationPrefs();
    mainWindow = new WindowPrefs();
    forgedAlliance = new ForgedAlliancePrefs();
    themeName = new SimpleStringProperty(DEFAULT_THEME_NAME);
    lastGameType = new SimpleStringProperty(KnownFeaturedMod.DEFAULT.getTechnicalName());
    ignoredNotifications = new SimpleListProperty<>(observableArrayList());
    notification = new NotificationsPrefs();
    rememberLastTab = new SimpleBooleanProperty(true);
    lastGameTitle = new SimpleStringProperty();
    lastMap = new SimpleStringProperty();
    lastGameMinRank = new SimpleIntegerProperty(800);
    lastGameMaxRank = new SimpleIntegerProperty(1300);
    ladder1v1 = new Ladder1v1Prefs();
    gamesViewMode = new SimpleStringProperty();
    news = new NewsPrefs();
    developer = new DeveloperPrefs();
    gameListSorting = new SimpleListProperty<>(observableArrayList());
    vaultPrefs = new VaultPrefs();
    unitDataBaseType = new SimpleObjectProperty<>(UnitDataBaseType.RACKOVER);
    storedCookies = new SimpleMapProperty<>(FXCollections.observableHashMap());
    showPasswordProtectedGames = new SimpleBooleanProperty(true);
    showModdedGames = new SimpleBooleanProperty(true);
    lastGameOnlyFriends = new SimpleBooleanProperty();
  }

  public VaultPrefs getVaultPrefs() {
    return vaultPrefs;
  }


  public TilesSortingOrder getGameTileSortingOrder() {
    return gameTileSortingOrder.get();
  }

  public void setGameTileSortingOrder(TilesSortingOrder gameTileTilesSortingOrder) {
    this.gameTileSortingOrder.set(gameTileTilesSortingOrder);
  }

  public ObjectProperty<TilesSortingOrder> gameTileSortingOrderProperty() {
    return gameTileSortingOrder;
  }

  public BooleanProperty showPasswordProtectedGamesProperty() {
    return showPasswordProtectedGames;
  }

  public BooleanProperty showModdedGamesProperty() {
    return showModdedGames;
  }

  public String getGamesViewMode() {
    return gamesViewMode.get();
  }

  public void setGamesViewMode(String gamesViewMode) {
    this.gamesViewMode.set(gamesViewMode);
  }

  public StringProperty gamesViewModeProperty() {
    return gamesViewMode;
  }

  public WindowPrefs getMainWindow() {
    return mainWindow;
  }

  public LocalizationPrefs getLocalization() {
    return localization;
  }

  public ForgedAlliancePrefs getForgedAlliance() {
    return forgedAlliance;
  }

  public LoginPrefs getLogin() {
    return login;
  }

  public ChatPrefs getChat() {
    return chat;
  }

  public NotificationsPrefs getNotification() {
    return notification;
  }

  public String getThemeName() {
    return themeName.get();
  }

  public void setThemeName(String themeName) {
    this.themeName.set(themeName);
  }

  public StringProperty themeNameProperty() {
    return themeName;
  }

  public String getLastGameType() {
    return lastGameType.get();
  }

  public void setLastGameType(String lastGameType) {
    this.lastGameType.set(lastGameType);
  }

  public StringProperty lastGameTypeProperty() {
    return lastGameType;
  }

  public String getLastGameTitle() {
    return lastGameTitle.get();
  }

  public void setLastGameTitle(String lastGameTitle) {
    this.lastGameTitle.set(lastGameTitle);
  }

  public StringProperty lastGameTitleProperty() {
    return lastGameTitle;
  }

  public String getLastMap() {
    return lastMap.get();
  }

  public void setLastMap(String lastMap) {
    this.lastMap.set(lastMap);
  }

  public StringProperty lastMapProperty() {
    return lastMap;
  }

  public boolean getRememberLastTab() {
    return rememberLastTab.get();
  }

  public void setRememberLastTab(boolean rememberLastTab) {
    this.rememberLastTab.set(rememberLastTab);
  }

  public BooleanProperty rememberLastTabProperty() {
    return rememberLastTab;
  }

  public ObservableList<String> getIgnoredNotifications() {
    return ignoredNotifications.get();
  }

  public void setIgnoredNotifications(ObservableList<String> ignoredNotifications) {
    this.ignoredNotifications.set(ignoredNotifications);
  }

  public ListProperty<String> ignoredNotificationsProperty() {
    return ignoredNotifications;
  }

  public int getLastGameMinRank() {
    return lastGameMinRank.get();
  }

  public void setLastGameMinRank(int lastGameMinRank) {
    this.lastGameMinRank.set(lastGameMinRank);
  }

  public IntegerProperty lastGameMinRankProperty() {
    return lastGameMinRank;
  }

  public int getLastGameMaxRank() {
    return lastGameMaxRank.get();
  }

  public void setLastGameMaxRank(int lastGameMaxRank) {
    this.lastGameMaxRank.set(lastGameMaxRank);
  }

  public IntegerProperty lastGameMaxRankProperty() {
    return lastGameMaxRank;
  }

  public Ladder1v1Prefs getLadder1v1Prefs() {
    return ladder1v1;
  }

  public NewsPrefs getNews() {
    return news;
  }

  public DeveloperPrefs getDeveloper() {
    return developer;
  }

  public ObservableList<Pair<String, SortType>> getGameListSorting() {
    return gameListSorting.get();
  }

  public UnitDataBaseType getUnitDataBaseType() {
    return unitDataBaseType.get();
  }

  public void setUnitDataBaseType(UnitDataBaseType unitDataBaseType) {
    this.unitDataBaseType.set(unitDataBaseType);
  }

  public ObjectProperty<UnitDataBaseType> unitDataBaseTypeProperty() {
    return unitDataBaseType;
  }

  public ObservableMap<URI, ArrayList<HttpCookie>> getStoredCookies() {
    return storedCookies.get();
  }

  public boolean isLastGameOnlyFriends() {
    return lastGameOnlyFriends.get();
  }

  public void setLastGameOnlyFriends(boolean lastGameOnlyFriends) {
    this.lastGameOnlyFriends.set(lastGameOnlyFriends);
  }

  public BooleanProperty lastGameOnlyFriendsProperty() {
    return lastGameOnlyFriends;
  }

  public enum UnitDataBaseType {
    SPOOKY("unitDatabase.spooky"),
    RACKOVER("unitDatabase.rackover");

    @Getter
    private final String i18nKey;

    UnitDataBaseType(String i18nKey) {
      this.i18nKey = i18nKey;
    }
  }
}