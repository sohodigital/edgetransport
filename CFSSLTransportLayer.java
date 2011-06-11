package edge;
import java.io.*;
import java.net.UnknownHostException;
import java.security.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.*;

public class CFSSLTransportLayer
{
      final String host = "edge.asic.gov.au";
      final int port = 5608;
      private static SSLSocket sslServerSocket;
      static boolean debug = true;
      final String keyStoreLocation ="C:\\Documents and Settings\\Administrator\\.keystore";
      final String password = "changeit";
      final int socketTimeout = 60000; // Set timeout to one minute as per specs.
      SSLSocketFactory factory;
      PrintWriter pw;
      BufferedReader in = null;
      String error = "";


      /*
      public static void main(String[] args) {
          CFSSLTransportLayer tl = new CFSSLTransportLayer();
          System.out.println( tl.send( "XSCLOGN\nS00042S00042          ", true ) );
          //System.out.println( tl.send( "ZHDASC201      0500      0004\nZCOSHELF COMPANY 1001 PTY LTD            APTY      LMSH            N                                          NSW            Y\nZRG            BROWN BLOGGS AND CO      SHOP 3 FRONT OFFICE      104 HOMEPRIDE AVE      WARWICK FARM      NSW      2170      Y\nZRP                              53 SEPTEMBER ST      STRATHFIELD      NSW      2135\nZUHJONES HOLDING COMPANY LIMITED      550111222      AUSTRALIA            Y\nZSD            JONES      ROBERTA                  19540308      SYDNEY      NSW                                                      23 CASTOR ST      EDGE HILL      QLD      4870\nZOFDIR\nZOFSEC\nZSD            SMITH      JOHN      WILLIAM            19411202      MELBOURNE      VIC                                                      53 SEPTEMBER ST      STRATHFIELD      NSW      2135            Y\nZOFDIR\nZHHA      2      N      Y            100      0\nZSHJONES      ROBERTA                                          23 CASTOR ST      EDGE HILL      QLD      4870\nZHHA      2      N      Y            100      0\nZSHSMITH      JOHN      WILLIAM                                    53 SEPTEMBER ST      STRATHFIELD      NSW      2135\nZSHSMITH      MARY                                          53 SEPTEMBER ST      STRATHFIELD      NSW      2135\nZCGBUTLER      GAVIN      JOHN                                    53 SEPTEMBER ST      STRATHFIELD      NSW      2135\nZDCBUTLER      GAVIN      JOHN                  20000901      Y\nZAM            PSCR\nZTREND201      19\nZXI/C=au/O=KeyPOST Test CA      100000470      MD5      RSA\nZXSTrDdTYrzGKy+n7WBgITczo9/0C3BlLszVo89dwGlFGDu42slbVPKsjkQaLcGruwk\nZXSv7vQVK/sMpEP8gcB3dIh2VCQ3DN8aQqSV23K9T7y4fstbWDnoi5+rdWCefZOwrZ8\nZXSqC1fhlkVzaRIWOc8J33I1dtCt2PirzSbqRtqXDLOtqI=\n", true ) );
          
          tl.closeConnection();
      }*/


      public CFSSLTransportLayer()
      {

            try
            {
                  KeyStore myKeyStore = KeyStore.getInstance( "JKS" );
                  myKeyStore.load( new FileInputStream( keyStoreLocation ), password.toCharArray() );
                  TrustManagerFactory myTrustFactory = TrustManagerFactory.getInstance( "SunX509", "SunJSSE" );
                  myTrustFactory.init( myKeyStore );
                  TrustManager[] myTrustManager = myTrustFactory.getTrustManagers();
                  SSLContext ctx = SSLContext.getInstance( "SSL" );
                  ctx.init( null, myTrustManager, null );
                  factory = ctx.getSocketFactory();
            }
            catch ( Exception e )
            {
                  String myError = "Error creating SSL objects.";
                  System.err.println( myError );
                  setError( myError );
                  e.printStackTrace( System.out );
            }

            try
            {
                  sslServerSocket = ( SSLSocket ) factory.createSocket( host, port );
                  // Set the timeout
                  sslServerSocket.setSoTimeout( socketTimeout );
                  if ( debug )
                  {
                        System.out.println( "*************************************************" );
                        System.out.println( "***********Secure socket made********************" );
                        System.out.println( "*************************************************" );
                        System.out.println();
                  }
                  pw = new PrintWriter( sslServerSocket.getOutputStream() );
            }
            catch ( UnknownHostException e )
            {
              String myError = "Unknown host.";
                  System.err.println( myError );
              setError( myError );
            }
            catch ( java.net.SocketTimeoutException e )
            {
                  String myError = "The Socket waited " + socketTimeout / 1000 + " seconds for a response.";
                  System.err.println( myError );
              setError( myError );
            }
            catch  ( IOException e )
            {
              String myError = "I/O Exception.";
                  System.err.println( myError );
              setError( myError );
        }
        catch ( Exception e )
            {
              String myError = "An exception has occurred, error message: " + e.getMessage();
                  System.err.println( myError );
              setError( myError );
        }
      }

      
      private void setError( String error )
      {
            this.error = this.error + error + "\n";
      }
      
      private String getError()
      {
            return this.error;
      }
      
      
      public String send ( String message, boolean isLast )
      {
            String response;
            String messageToSend = "BDAT " + message.length();
            if ( isLast )
            {
                  messageToSend = messageToSend + " LAST"; 
            }
            messageToSend = messageToSend + "\r\n" + message;
            // Sent the message to EDGE
            pw.print( messageToSend );
            pw.flush();
            response = getResponse();
            if ( debug )
            {
                  System.out.println( "Message that was sent to EDGE " + messageToSend );
                  System.out.println( "Message sent to EDGE." );
                  response = "response = " + response + getError();
            }
            return response;
      }


      private String getResponse()
      {
            String messageString = "";
            try
            {
                  if ( debug )
                  {
                        System.out.println( "*************************************************" );
                        System.out.println( "************Server Output************************" );
                        System.out.print( "Outputting reply from server: " );
                  }

                  in = new BufferedReader( new InputStreamReader( sslServerSocket.getInputStream() ) );

                  String header = "";
                  char[] myChar = new char[ 1 ];
                  // Making sure that we do not enter an infinite loop
                  int i = 0;
                  final int MAX_LENGTH_TO_READ = 30;
                  // Loop until we find the newline character, this should give us the header
                  while ( !header.endsWith( "\n" ) && i < MAX_LENGTH_TO_READ ) 
                  {
                        in.read( myChar, 0, 1 );
                        String myStringchar = new String( myChar );
                        header += myStringchar;
                        i++;
                  }
                  if ( i == MAX_LENGTH_TO_READ )
                  {
                        String myError = "Could not read the header!";
                        System.err.println( myError );
                    setError( myError );
                  }

                  int byteToRead = 0;
                  // Parse out the message length
                  Pattern myPattern = Pattern.compile( "\\d+" );
                  Matcher myMatch = myPattern.matcher( header );
                  if ( myMatch.find() )
                  {
                        byteToRead = Integer.parseInt( header.substring( myMatch.start(), myMatch.end() ) );
                  }
                  else
                  {
                        String myError = "Message length could not be found in the header!";
                        System.err.println( myError );
                    setError( myError );
                        // take your action in case no integer found in yourString
                        throw new Exception("MessageLengthNotFound", new Throwable( "The message length could not be found in the header." ) );
                  }

                  char[] myMessage = new char[ byteToRead ];
            // Try and read the rest of the message
            try
                  {
                  int remaining = byteToRead;
                  // Loop untill all bytes are read
                  while ( remaining > 0 )
                  {
                        remaining -= in.read( myMessage, byteToRead - remaining, remaining );
                  }
                  }
            catch ( Exception e )
                  {
                  String myError = "Error reading the rest of the message, byteToRead: " + byteToRead + ". message: " + e.getMessage() + "\n";
                        System.err.println( myError );
                    setError( myError );
            }
            // Convert the message array to a string
            messageString = new String( myMessage );
            System.out.println( messageString );

                  if ( debug )
                  {
                        System.out.println( "***********Server output complete****************" );
                        System.out.println( "*************************************************" );
                        System.out.println();
                  }
            }
            catch ( IOException e )
            {
                  String myError = "There was an error reading the Input Stream. The error is " + e.getMessage();
                  System.err.println( myError );
              setError( myError );
            }
            catch ( Exception e )
            {
                  e.printStackTrace( System.out );
            }
            return messageString;
      }


      public void closeConnection()
      {
            try
            {
              in.close();
              pw.close();
              // close socket
              sslServerSocket.close();
            }
        catch ( IOException e )
            {
              System.out.println( "Error trying to close the socket" + e.getMessage() + "\n" );
            }
      }
      
      protected void finalize() 
      throws Throwable 
      {
            try 
            {
                  closeConnection();
          } 
            finally 
            {
                  super.finalize();
            }
      }

}