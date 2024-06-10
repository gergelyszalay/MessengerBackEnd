package com.gege.ideas.messenger.conversation.service;

import com.gege.ideas.messenger.conversation.ConversationKeys;
import com.gege.ideas.messenger.conversation.entity.ConversationParticipant;
import com.gege.ideas.messenger.conversation.entity.Conversations;
import com.gege.ideas.messenger.conversation.repository.ConversationsRepository;
import com.gege.ideas.messenger.message.service.MessageService;
import com.gege.ideas.messenger.user.entity.User;
import com.gege.ideas.messenger.user.service.UserService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConversationsService {

   private UserService userService;

   private ConversationParticipantsService conversationParticipantsService;

   private MessageService messageService;
   private ConversationsRepository conversationsRepository;

   @Autowired
   public ConversationsService(
      UserService userService,
      ConversationParticipantsService conversationParticipantsService,
      MessageService messageService,
      ConversationsRepository conversationsRepository
   ) {
      this.userService = userService;
      this.conversationParticipantsService = conversationParticipantsService;
      this.messageService = messageService;
      this.conversationsRepository = conversationsRepository;
   }

   public HashMap getConversation(long id) {
      HashMap conversation = new HashMap();

      conversation.put(ConversationKeys.ID, id);

      List<User> participantUsers = new ArrayList<>();
      List<Long> participantIds =
         conversationParticipantsService.getParticipantIds(id);

      for (Long participantId : participantIds) {
         participantUsers.add(userService.getUser(participantId));
      }
      conversation.put(ConversationKeys.USERS, participantUsers);
      conversation.put(
         ConversationKeys.MESSAGES,
         messageService.getConversationMessages(id)
      );

      return conversation;
   }

   public Long addConversation(List<User> participants) {
      Long conversationId = _getExistingConversationId(participants);
      if (conversationId == null) {
         Conversations conversations = new Conversations(
            System.currentTimeMillis()
         );
         conversationsRepository.save(conversations);
         for (User participant : participants) {
            ConversationParticipant conversationParticipant =
               new ConversationParticipant(
                  conversations.getConversationId(),
                  participant.getUserId()
               );
            conversationParticipantsService.addConversationParticipant(
               conversationParticipant
            );
         }
         return conversations.getConversationId();
      }
      return conversationId;
   }

   private Long _getExistingConversationId(List<User> participants) {
      User user = participants.get(0);

      List<Long> conversationIds =
         conversationParticipantsService.getConversationIdsByUserId(
            user.getUserId()
         );

      for (Long conversationId : conversationIds) {
         List<Long> participantIds =
            conversationParticipantsService.getParticipantIds(conversationId);
         if (participants.size() == participantIds.size()) {
            for (User participant : participants) {
               if (
                  participantIds.contains(participant.getUserId()) &&
                  !participant.equals(participants.get(0))
               ) {
                  return conversationId;
               }
            }
         }
      }

      return null;
   }
}
