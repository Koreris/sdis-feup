/****************** README ******************/

~~~~~~~~~~ Para compilar:

Gerar os ficheiros .class a partir do código-fonte no IDE preferido. 


~~~~~~~~~~ Para iniciar um servidor:

A pasta backup, contendo os ficheiros .class, que se encontra na pasta bin após fazer
build do projecto deve ser colocada no ambiente de trabalho do computador.

Ainda no ambiente de trabalho deve também existir um diretório sdis com a seguinte estrutura

├── sdis
|   └── files
|       └── <peername>

Dentro do diretório <peername> devem estar os ficheiros para se fazer backup e deve-se
iniciar um servidor com o mesmo <peername> que esse diretório (os outros são criados automaticamente)

java -classpath <path_to_desktop> backup.MulticastServer <peername> <protocol_version> <rmi_object> <MDB> <MDB Port> <MDR> <MDR Port> <MC> <MC Port> <storage em KBs>

Ex:
java -classpath C:\Users\up201306229\Desktop backup.MulticastServer peer2 1.0 access2 239.0.0.0 7777 239.0.0.1 8888 239.0.0.2 9999 10000

Nota: Para testar os enhancements, deverá ser colocado no protocol_version o valor 2.0 no peer iniciador do protocolo. 


~~~~~~~~~~ Para iniciar um protocolo como cliente:

java -classpath <path_to_desktop> backup.MulticastClient //<hostname>/<rmi_object> <protocol> *<opnd1> *<opnd2> 

Ex:
java -classpath C:\Users\up201306229\Desktop backup.MulticastClient //localhost/access1 backup /sdis/files/peer1/p.jpg (fazer backup do ficheiro p.jpg necessita 
de ter o caminho a partir da pasta sdis e no código ele assume que este diretório sdis se encontra no ambiente de trabalho)







