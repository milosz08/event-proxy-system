import * as React from 'react';
import { useEffect, useMemo, useState } from 'react';
import notificationSound from '../assets/notification.mp3';

function App(): React.ReactElement {
  const audio = useMemo(() => new Audio(notificationSound), []);
  const [messages, setMessages] = useState<string[]>([]);

  useEffect(() => {
    const removeOnPongListener = window.api.onPong(async (message: string) => {
      audio.currentTime = 0;
      await audio.play();
      setMessages(prevState => [...prevState, message]);
    });
    const removeOnClearedPingListener = window.api.onClearedPings(() => {
      setMessages([]);
    })
    return () => {
      removeOnPongListener();
      removeOnClearedPingListener();
    };
  }, [audio]);

  return (
    <div>
      <button onClick={() => window.api.sendPing()}>send ping!</button>
      <button onClick={() => window.api.clearPings()}>clear pings</button>
      <ul>
        {messages.map((message, index) => (
          <li key={index}>{message}</li>
        ))}
      </ul>
    </div>
  );
}

export default App;
