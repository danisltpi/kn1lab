import java.io.*;
import java.net.*;
import java.time.Duration;
import java.util.concurrent.*;

/**
 * Die "Klasse" Sender liest einen String von der Konsole und zerlegt ihn in einzelne Worte. Jedes Wort wird in ein
 * einzelnes {@link Packet} verpackt und an das Medium verschickt. Erst nach dem Erhalt eines entsprechenden
 * ACKs wird das nächste {@link Packet} verschickt. Erhält der Sender nach einem Timeout von einer Sekunde kein ACK,
 * überträgt er das {@link Packet} erneut.
 */
public class Sender {
    /**
     * Hauptmethode, erzeugt Instanz des {@link Sender} und führt {@link #send()} aus.
     * @param args Argumente, werden nicht verwendet.
     */
    public static void main(String[] args) {
        Sender sender = new Sender();
        try {
            sender.send();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Erzeugt neuen Socket. Liest Text von Konsole ein und zerlegt diesen. Packt einzelne Worte in {@link Packet}
     * und schickt diese an Medium. Nutzt {@link SocketTimeoutException}, um eine Sekunde auf ACK zu
     * warten und das {@link Packet} ggf. nochmals zu versenden.
     * @throws IOException Wird geworfen falls Sockets nicht erzeugt werden können.
     */
    private void send() throws IOException {
       //Text einlesen und in Worte zerlegen
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    String input = reader.readLine();
    input += " EOT";
    String[] words = input.split(" ");



    // Socket erzeugen auf Port 9998 und Timeout auf eine Sekunde setzen
        DatagramSocket clientSocket = new DatagramSocket(9998);
        clientSocket.setSoTimeout(1000);
        // Iteration über den Konsolentext

        int i = 0;
        int seq = 0;
      while(i != words.length) {
            //Create Packet for every word
            Packet packetOut = new Packet(seq,i,false,words[i].getBytes());
            //Serialize Packet for sending
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(b);
            o.writeObject(packetOut);
            byte[] buf = b.toByteArray();
            //create actual sendable DatagrammPacket
            DatagramPacket toSend = new DatagramPacket(buf,buf.length,InetAddress.getLocalHost(),9997 );

            //send Packet
            clientSocket.send(toSend);

            try {
                byte[] bufRec = new byte[256];
                DatagramPacket toReceive = new DatagramPacket(bufRec, bufRec.length);
                clientSocket.receive(toReceive);

                //deserialization
                ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(toReceive.getData()));
                Packet packetIn = (Packet) is.readObject();
                //check if Acknum is correctly incremented by receiver
                if(packetIn.isAckFlag() && (packetIn.getAckNum()== seq + packetIn.getPayload().length)) {
                    i ++;
                    seq += packetIn.getPayload().length;
                }

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (SocketTimeoutException e) {
                System.out.println("Receive timed out, retrying...");
            }
        }
        clientSocket.close();

        if(System.getProperty("os.name").equals("Linux")) {
            clientSocket.disconnect();
        }
        System.exit(0);
    }
}