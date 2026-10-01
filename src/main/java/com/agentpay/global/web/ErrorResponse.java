package com.agentpay.global.web;

/** 에이전트가 파싱하기 쉬운 고정 형태의 오류 응답. code는 기계 판독용, message는 사람/LLM용. */
public record ErrorResponse(String code, String message) {
}
