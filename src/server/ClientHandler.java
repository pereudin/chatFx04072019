package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    DataOutputStream out;
    DataInputStream in;
    String nick;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    // цикл авторизации.
                    while (true) {
                        String str = in.readUTF();
                        if(str.startsWith("/auth")){
                            String[] token = str.split(" ");
                            String newNick =
                                    AuthService.getNickByLoginAndPass(token[1],token[2]);
                            if(newNick != null && !server.reauthorizationCheck(token[1], token[2])){
                                sendMsg("/authok");
                                nick = newNick;
                                server.subscribe(this);
                                break;
                            }else if(server.reauthorizationCheck(token[1], token[2])) {
                                sendMsg("Пользователь с таким логином уже авторизован");
                            }else {
                                sendMsg("Неверный логин / пароль");
                            }
                        }
                    }

                    //Цикл для работы
                    while (true) {
                        String str = in.readUTF();

                        if(str.startsWith("/w")){
                            String[] token = str.split(" ", 3);
                            server.directedMsg(token[1], "Приватное сообщение от " + nick + ": \n" + token[2]);
                            out.writeUTF(nick + " приватно для " + token[1] + ": \n" + token[2]);
                        } else {
                            System.out.println(str);
                            server.broadcastMsg(nick + ": " +str);
                        }

                        if (str.equals("/end")) {
                            out.writeUTF("/end");
                            System.out.println("Клиент отключился");
                            break;
                        }


                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    server.unsubscribe(this);
                    System.out.println("Клиент отключился");
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String str) {
        try {
            out.writeUTF(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
