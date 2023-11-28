package com.oio.chatservice.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oio.chatservice.dto.ChatDto;
import com.oio.chatservice.dto.ChatRoomDto;
import com.oio.chatservice.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final ChatService messageService;
    private final ObjectMapper objectMapper;

    public WebSocketHandler(ChatService messageService, ObjectMapper objectMapper) {
        this.messageService = messageService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 클라이언트로부터 텍스트 메시지를 받았을 때 호출

        // 클라이언트로부터 받은 메시지의 내용을 추출
        String payload = message.getPayload();
        log.info("payload: {}", payload);

        // JSON 형식의 메시지를 MessageDto 객체로 변환
        ChatDto messageDto = objectMapper.readValue(payload, ChatDto.class);
        log.info("messageDto: {}", messageDto);

        // 메시지에서 얻은 방 ID를 이용해 해당 채팅 방을 찾아줌
        ChatRoomDto messageRoomDto = messageService.findById(messageDto.getRoomId());
        log.info("Found MessageRoomDto for roomId {}: {}", messageDto.getRoomId(), messageRoomDto);

        messageRoomDto.handleActions(session, messageDto, messageService);

    } // handleTextMessage()

} // WebSocketHandler()