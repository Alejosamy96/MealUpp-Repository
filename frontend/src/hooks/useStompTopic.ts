// Suscripción en tiempo real (STOMP sobre WebSocket) para las pantallas del personal.
// Si no conecta, la pantalla sigue funcionando con su recarga periódica.
import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import { wsUrl } from "../api/client";
import { session } from "../services/session";

export type SocketState = "connecting" | "live" | "offline";

export function useStompTopic<T>(topic: string, onMessage: (msg: T) => void, onConnect?: () => void): SocketState {
  const [state, setState] = useState<SocketState>("connecting");
  const handler = useRef(onMessage);
  const connectHandler = useRef(onConnect);
  handler.current = onMessage;
  connectHandler.current = onConnect;

  useEffect(() => {
    const token = session.token();
    if (!token) {
      setState("offline");
      return;
    }
    const client = new Client({
      brokerURL: wsUrl(),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setState("live");
        connectHandler.current?.();
        client.subscribe(topic, (frame) => {
          try {
            handler.current(JSON.parse(frame.body) as T);
          } catch {
            /* mensaje con formato inesperado: se ignora */
          }
        });
      },
      onStompError: () => setState("offline"),
      onWebSocketClose: () => setState((s) => (s === "live" ? "connecting" : "offline")),
      onWebSocketError: () => setState("offline"),
    });
    client.activate();
    return () => {
      void client.deactivate();
    };
  }, [topic]);

  return state;
}
