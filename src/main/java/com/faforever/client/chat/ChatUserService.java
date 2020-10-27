package com.faforever.client.chat;

import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.chat.event.ChatUserAvatarEvent;
import com.faforever.client.chat.event.ChatUserClanEvent;
import com.faforever.client.chat.event.ChatUserCountryEvent;
import com.faforever.client.chat.event.ChatUserStatusEvent;
import com.faforever.client.clan.ClanService;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.theme.UiService;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatUserService implements InitializingBean {

  private final UiService uiService;
  private final MapService mapService;
  private final AvatarService avatarService;
  private final ClanService clanService;
  private final CountryFlagService countryFlagService;
  private final I18n i18n;
  private final EventBus eventBus;

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
  }

  @Subscribe
  public void onChatUserClan(ChatUserClanEvent event) {
    ChatChannelUser chatChannelUser = event.getChatChannelUser();
    chatChannelUser.getPlayer().ifPresent(player -> Platform.runLater(() -> clanService.getClanByTag(player.getClan()).thenAccept(optionalClan -> {
      if (optionalClan.isPresent()) {
        chatChannelUser.setClan(optionalClan.get());
      } else {
        chatChannelUser.setClan(null);
      }
    })));
  }

  @Subscribe
  public void onChatUserAvatar(ChatUserAvatarEvent event) {
    ChatChannelUser chatChannelUser = event.getChatChannelUser();
    chatChannelUser.getPlayer()
        .ifPresent(player -> Platform.runLater(() -> {
          if (!Strings.isNullOrEmpty(player.getAvatarUrl())) {
            chatChannelUser.setAvatar(avatarService.loadAvatar(player.getAvatarUrl()));
          } else {
            chatChannelUser.setAvatar(null);
          }
        }));
  }

  @Subscribe
  public void onChatUserStatus(ChatUserStatusEvent event) {
    ChatChannelUser chatChannelUser = event.getChatChannelUser();
    chatChannelUser.getPlayer()
        .ifPresent(player -> Platform.runLater(() -> {
          Image playerStatusImage = switch (player.getStatus()) {
            case HOSTING -> uiService.getThemeImage(UiService.CHAT_LIST_STATUS_HOSTING);
            case LOBBYING -> uiService.getThemeImage(UiService.CHAT_LIST_STATUS_LOBBYING);
            case PLAYING -> uiService.getThemeImage(UiService.CHAT_LIST_STATUS_PLAYING);
            default -> null;
          };
          chatChannelUser.setStatusImage(playerStatusImage);

          if (player.getStatus() != PlayerStatus.IDLE) {
            chatChannelUser.setMapImage(mapService.loadPreview(player.getGame().getMapFolderName(), PreviewSize.SMALL));
          } else {
            chatChannelUser.setMapImage(null);
          }
        }));
  }

  @Subscribe
  public void onChatUserCountry(ChatUserCountryEvent event) {
    ChatChannelUser chatChannelUser = event.getChatChannelUser();
    chatChannelUser.getPlayer()
        .ifPresent(player -> Platform.runLater(() -> {
          Optional<Image> countryFlag = countryFlagService.loadCountryFlag(player.getCountry());
          if (countryFlag.isPresent()) {
            chatChannelUser.setCountryFlag(countryFlag.get());
          } else {
            chatChannelUser.setCountryFlag(null);
          }
          chatChannelUser.setCountryName(i18n.getCountryNameLocalized(player.getCountry()));
        }));
  }
}


