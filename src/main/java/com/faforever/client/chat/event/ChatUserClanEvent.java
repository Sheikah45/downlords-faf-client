package com.faforever.client.chat.event;

import com.faforever.client.chat.ChatChannelUser;
import lombok.Value;

@Value
public class ChatUserClanEvent {
  ChatChannelUser chatChannelUser;
}
