import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

export function useOrderSocket(destination: string | null, onEvent: () => void) {
  const [connected, setConnected] = useState(false);
  const callback = useRef(onEvent);
  callback.current = onEvent;

  useEffect(() => {
    if (!destination) return;
    const client = new Client({
      webSocketFactory: () => new SockJS("/ws"),
      reconnectDelay: 4000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(destination, () => callback.current());
      },
      onWebSocketClose: () => setConnected(false),
      onStompError: () => setConnected(false)
    });
    client.activate();
    return () => {
      setConnected(false);
      void client.deactivate();
    };
  }, [destination]);

  return connected;
}
