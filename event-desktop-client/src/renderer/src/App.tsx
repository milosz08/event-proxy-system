import * as React from 'react';
import { useEffect, useMemo, useState } from 'react';
import notificationSound from '../assets/notification.mp3';

function App(): React.ReactElement {
  const audio = useMemo(() => new Audio(notificationSound), []);
  const [messages, setMessages] = useState<string[]>([]);

  useEffect(() => {
    const removeListener = window.api.onPong(async (message: string) => {
      audio.currentTime = 0;
      await audio.play();
      setMessages(prevState => [...prevState, message]);
    });

    return () => {
      removeListener();
    };
  }, [audio]);

  return (
    <div>
      <button onClick={() => window.api.sendPing()}>send ping!</button>
      <ul>
        {messages.map((message, index) => (
          <li key={index}>{message}</li>
        ))}
      </ul>
    </div>
  );
}

export default App;
