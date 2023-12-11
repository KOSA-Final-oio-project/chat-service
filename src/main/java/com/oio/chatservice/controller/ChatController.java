package com.oio.chatservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oio.chatservice.dto.ChatDto;
import com.oio.chatservice.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ChatController {

    // 특정 메시지 브로커에 메시지를 보내는 데 사용되는 스프링 서비스임. 특정 토픽에 구독한 클라이언트들에게 메시지를 보내는 것을 관리
    private final SimpMessageSendingOperations template;
    private final ChatService chatService;

    /*
     * 채팅 메시지를 처리하고, 해당 채팅방 구독자들에게 메시지를 전송
     * @param chatDto
     * 채팅 메시지의 종류(입장, 퇴장, 일반 메시지 등)에 따라 적절한 메시지 처리를 수행
     * 예를 들어, 사용자가 채팅방에 입장하는 경우, 해당 사용자의 입장 메시지를 생성해줌.
     * 이후, STOMP 메시지 브로커를 통해 해당 채팅방을 구독하고 있는 클라이언트들에게 메시지를 전송하는 역할을 수행함
     */
    @MessageMapping(value = "/chat/message")
    public void message(@Payload ChatDto chatDto) throws IOException {

        // 입장 처리
        if (ChatDto.MessageType.ENTER.equals(chatDto.getMessageType())) {
            chatDto.setMessage(" ' " + chatDto.getSender() + " '님이 입장하셨습니다.");
            // 셍나: ' 셍나 '님이 입장하셨습니다.
        } else if (ChatDto.MessageType.QUIT.equals(chatDto.getMessageType())) {
            chatDto.setMessage(" ' " + chatDto.getSender() + " '님이 퇴장하셨습니다.");
            // 셍나: ' 셍나 '님이 퇴장하셨습니다.
        } // if

        // ISO 8601 UTC 날짜 문자열을 받아 로컬 날짜/시간으로 변환
        LocalDateTime sendDate = LocalDateTime.ofInstant(
                Instant.parse(chatDto.getSendDate()),
                ZoneId.systemDefault()
        );

        // 원하는 형식으로 날짜/시간 포매팅
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedSendDate = sendDate.format(formatter);

        // 변환된 날짜/시간을 chatDto에 설정
        chatDto.setSendDate(formattedSendDate);

        // chatDto 페이로드를 특정 채팅방 토픽으로 전송 -> 해당 토픽에 구독한 모든 클라이언트(즉, 그 채팅방에 현재 있는 사용자들)는 이 메시지를 받음
        template.convertAndSend("/sub/chat/room/" + chatDto.getRoomId(), chatDto);

        chatService.saveChatToText(chatDto); // 채팅 메시지를 파일에 저장

    } // message()

} // end class

/**
 *         // convertAndSend
 *         // 지정된 목적지로 메시지를 보내는 데 사용
 *         // 1. 변환(Convert): 메시지 페이로드를 네트워크를 통해 보내기에 적합한 직렬화된 형태(일반적으로 JSON)로 변환함.
 *         // 2. 전송(Send): 직렬화된 메시지를 주어진 목적지로 전송
 *         template.convertAndSend("/sub/chat/room/" + chatDto.getRoomId(), chatDto);
 *         // /sub/chat/room => 메시지가 전송되는 목적지로 설정
 *         // 채팅방 ID에 기반한 동적으로 구성된 토픽!
 *         // 예를 들어, 각 채팅방은 고유한 토픽(예: /sub/chat/room/1, /sub/chat/room/2 등)을 가지며,
 *         // 특정 방의 메시지를 받고자 하는 클라이언트들은 이러한 토픽에 구독!
 */