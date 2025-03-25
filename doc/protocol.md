# Protocol description

This client-server protocol describes the following scenarios:
- Setting up a connection between client and server.
- Broadcasting a message to all connected clients.
- Periodically sending heartbeat to connected clients.
- Disconnection from the server.
- Handling invalid messages.

In the description below, `C -> S` represents a message from the client `C` is send to server `S`. When applicable, `C` is extended with a number to indicate a specific client, e.g., `C1`, `C2`, etc. The keyword `others` is used to indicate all other clients except for the client who made the request. Messages can contain a JSON body. Text shown between `<` and `>` are placeholders.

The protocol follows the formal JSON specification, RFC 8259, available on https://www.rfc-editor.org/rfc/rfc8259.html

# 1. Establishing a connection

The client first sets up a socket connection to which the server responds with a welcome message. The client supplies a username on which the server responds with an OK if the username is accepted or an ERROR with a number in case of an error.
_Note:_ A username may only consist of characters, numbers, and underscores ('_') and has a length between 3 and 14 characters.

## 1.1 Happy flow

client.Client sets up the connection with server.
```
S -> C: READY {"version": "<server version number>"}
```
- `<server version number>`: the semantic version number of the server.

After a while when the client logs the user in:
```
C -> S: ENTER {"username":"<username>"}
S -> C: ENTER_RESP {"status":"OK"}
```

- `<username>`: the username of the user that needs to be logged in.
      To other clients (Only applicable when working on Level 2):
```
S -> others: JOINED {"username":"<username>"}
```

## 1.2 Unhappy flow
```
S -> C: ENTER_RESP {"status":"ERROR", "code":<error code>}
```      
Possible `<error code>`:

| Error code | Description                              |
|------------|------------------------------------------|
| 5000       | User with this name already exists       |
| 5001       | Username has an invalid format or length |      
| 5002       | Already logged in                        |

# 2. Broadcast message

Sends a message from a client to all other clients. The sending client does not receive the message itself but gets a confirmation that the message has been sent.

## 2.1 Happy flow

```
C -> S: BROADCAST_REQ {"message":"<message>"}
S -> C: BROADCAST_RESP {"status":"OK"}
```
- `<message>`: the message that must be sent.

Other clients receive the message as follows:
```
S -> others: BROADCAST {"username":"<username>","message":"<message>"}   
```   
- `<username>`: the username of the user that is sending the message.

## 2.2 Unhappy flow

```
S -> C: BROADCAST_RESP {"status": "ERROR", "code": <error code>}
```
Possible `<error code>`:

| Error code | Description            |
|------------|------------------------|
| 6000       | User is not logged in  |

# 3. Heartbeat message

Sends a ping message to the client to check whether the client is still active. The receiving client should respond with a pong message to confirm it is still active. If after 3 seconds no pong message has been received by the server, the connection to the client is closed. Before closing, the client is notified with a HANGUP message, with reason code 7000.

The server sends a ping message to a client every 10 seconds. The first ping message is send to the client 10 seconds after the client is logged in.

When the server receives a PONG message while it is not expecting one, a PONG_ERROR message will be returned.

## 3.1 Happy flow

```
S -> C: PING
C -> S: PONG
```     

## 3.2 Unhappy flow

```
S -> C: HANGUP {"reason": <reason code>}
[Server disconnects the client]
```      
Possible `<reason code>`:

| Reason code | Description      |
|-------------|------------------|
| 7000        | No pong received |    

```
S -> C: PONG_ERROR {"code": <error code>}
```
Possible `<error code>`:

| Error code | Description         |
|------------|---------------------|
| 8000       | Pong without ping   |    

# 4. Termination of the connection

When the connection needs to be terminated, the client sends a bye message. This will be answered (with a BYE_RESP message) after which the server will close the socket connection.

## 4.1 Happy flow
```
C -> S: BYE
S -> C: BYE_RESP {"status":"OK"}
[Server closes the socket connection]
```

Other, still connected clients, clients receive:
```
S -> others: LEFT {"username":"<username>"}
```

## 4.2 Unhappy flow

- None

#  5. User list request

If a user wants to see the list of other logged-in users connected to the server,
the client sends a `USER_LIST_REQ` message.
The server responds with a `USER_LIST_RESP` message
containing an array of usernames of the currently connected and logged-in users.
If no other users are connected to the server, the `users` array in the response will be empty.


## 5.1 Happy flow

The server responds with a list of usernames if other logged-in users are connected to the server.

```
C -> S: USER_LIST_REQ 
S -> C: USER_LIST_RESP {"status":"OK", "users":["<username1>", "<username2>", .."]}
```
Or with the empty array if no other users are connected to the server and logged in.

```
S -> C: USER_LIST_RESP {"status":"OK", "users":[]}
```

## 5.2 Unhappy flow

```
S -> C: USER_LIST_RESP {"status":"ERROR", "code":<error code>}
```

Possible `<error code>`:

| Error code | Description            |
|------------|------------------------|
| 6000       | User is not logged in  |

# 6. Private message

Allows a user to send a private message to a specific recipient by sending a `PRIVATE_MSG_REQ` command.
The server delivers the message to the specified recipient,
who receives a `PRIVATE_MSG` message containing the sender's username and the message content.

## 6.1 Happy flow

```
C -> S: PRIVATE_MSG_REQ {"recipient": "<username>", "message": "<message>"}
```
 - `<username>:` The username of the recipient.
 - `<message>:` The content of the private message.

```
S -> C: PRIVATE_MSG_RESP {"status": "OK"}
```
- `status:` Indicates the success of the message delivery.
```
S -> C (Recipient): PRIVATE_MSG {"sender": "<username>", "message": "<message>"}
```
- `<username>:` The username of the sender.
- `<message>:` The content of the private message.

## 6.2 Unhappy flow

```
S -> C: PRIVATE_MSG_RESP {"status": "ERROR", "code": <error code>}
```
Possible `<error code>`:

| Error code | Description                  |
|------------|------------------------------|
| 6000       | User sender is not logged in |
| 6004       | Recipient user not found     |



# 7. Rock/Paper/Scissors game

Allows a user to start and play a Rock, Paper, Scissors game with another user.  
The game involves a series of commands exchanged between the client and server to initiate the game,
allow players to make their moves, and notify both participants of the result.

To start the game, the initiating user sends a `GAME_START_REQ` command.
Players make their moves by sending the `GAME_MOVE` command.
Once both players have sent their moves,
the server processes the game and sends the results to both participants using the `GAME_RESULT` command.

## 7.1 Happy flow
The initiating user sends a `GAME_START_REQ` command to the server to start the game with another user.

**Starting of the game:**
```
C -> S: GAME_START_REQ {"opponent": "<username>"}
```
- `<username>`: The username of the opponent user.

If the request to start the game is successful, the server sends a response with a status of `OK`,
and the opponent receives a notification about the game initiation.

```
S -> C (initiator): GAME_START_RESP {"status": "OK"}
```

```
S -> C (opponent): GAME_INVITATION {"initiator": "<username>"}
```
- `<username>`: The username of the initiator user.

The client specified
as the opponent in the GAME_START_REQUEST is notified by the server with a game invitation
once the server successfully processes the request.
This informs the opponent that they have been invited to participate in the game.

```
S -> C (both players): GAME_NOTIFICATION {"initiator": "<initiator username>", "opponent": "<opponent username>"}

```
- `<initiator username>`: The username of the initiator user.
- `<opponent username>`: The username of the opponent user.


After the invitation is sent to the opponent, the game officially starts.
Both players receive a game notification
indicating that the game has started between them and that they can now submit their moves.

**Moves from participants of the game:**

When a user enters the move they want to submit, a message in the following format is sent to the server.

```
C -> S: GAME_MOVE {"moveCode": "<code>"}
```
 - `<code>`: Code for option of "R-rock", "P-paper", or "S-scissors".


## Happy flow for move
```
S -> C: GAME_MOVE_RESP {"status": "OK"}
```
If the user submits a valid move for an active game they are participating in, they receive an OK response, 
indicating that the move was successfully submitted, and they should wait for the game result.


## Unhappy flow for move

```
S -> C: GAME_MOVE_RESP {"status": "ERROR", "code": <error code>}
```
Possible `<error code>`:

| Error code | Description                                            |
|------------|--------------------------------------------------------|
| 9001       | Invalid move (not `rock`, `paper`, or `scissors`)      | 
| 9002       | No active game on server, the move cannot be submitted | 
| 9003       | User are not a participant in the current game         | 
| 9004       | You have already made your move                        | 



**Game result:**

Once the server receives moves from both players, it calculates the winner and sends the results to both participants.

```
S -> C (both players): GAME_RESULT {"winner": "<username>", "initiatorMove": "<moveCode>", "opponentMove": "<moveCode>"}
```

- `<username>`: The username of the winner (f the `username` is null, then the game result is a draw.).
- `<moveCode>`: The moveCode corresponds to selected move by each player (e.g., "R-rock", "P-paper", "S-scissors").

## 7.2 Unhappy flow

When a user sends a request to start the game, in the unhappy flow,
the server responds with an error message containing a specific error code explaining why the game cannot be started.

When a game cannot be started due to either the initiator not being logged in or the opponent not being found,
the server will respond with a message of this format:

```
S -> C (initiator): GAME_START_RESP {"status": "ERROR", "code": <error code>}
```
When a game cannot be started because another game is already in progress between two other users,
the server responds with an additional username fields.

```
S -> C: GAME_START_RESP {"status": "ERROR", "code": <error code>, "usernameA": "<usernameA>", "usernameB": "<usernameB>"}
```
- `<usernameA>`: The username user A who involved in game.
- `<usernameB>`: The username of user B who involved in game.


Possible `<error code>`:


| Error code | Description                                        |
|------------|----------------------------------------------------|
| 6000       | User initiator is not logged in.                   |
| 6004       | Opponent user not found.                           |      
| 9000       | A game is already running between two other users. |




**Game cancellation**

If one user disconnects or leaves during the game, or if one or both players fail to submit their moves within the time frame of 60 seconds,
the server notifies the remaining participant (in case of disconnection) or both players (in case of failure to submit a move) that the game has been canceled due to one of the reasons specified below.

```
S -> C: GAME_CANCELLED { "errorCode": <error code>}
```
Possible `<error code>`:

| Error code | Description                                               |
|------------|-----------------------------------------------------------|
| 9005       | The opponent disconnects during the game                  |
| 9006       | One or both players failed to submit their moves in time. |




# 8. File transfer

This section describes the process for transferring files between clients through the server.
The handshake between users, utilizing messages, is performed on the main server on port `1337` and described below.
The actual file transfer is performed on a separate server running on port `1338` (which is +1 to the initial server port),
and this is done through a separate connection to avoid blocking the main communication flow.

## 8.1 Happy flow

The sender sends a file transfer request to the server on the main server port 1337. The server processes the request and forwards it to the intended receiver, 
replacing the receiver field with the sender field.



```
S(Sender) -> S: FILE_TRANSFER_REQ {"receiver": "<receiver>", "filename": "<filename>", "filesize": <size>, "cheksum:" "<cheksum>"}
```

```
S -> R (Receiver): FILE_TRANSFER_REQ {"sender": "<sender>", "filename": "<filename>", "filesize": <size>, "cheksum:" "<cheksum>"}
```

- `<receiver>`: The username of the intended file receiver.
- `<sender>`: The username of the file sender.
- `<filename>`: Name of the file being transferred.
- `<filesize>`: Total size of the file (in bytes).
- `<checksum>`: The hash of the file generated for the receiver for checksum, created using SHA-256.


After the receiver receives the file transfer request, they can either accept or reject it.


**Acceptance (Happy Flow)**

If the recipient accepts the file transfer, they specify the user from whom they want to accept the file transfer.
The next message is sent to the server.

```
R (Receiver) -> S: FILE_TRANSFER_ACCEPT {"sender": "<sender>"}
```
- `<sender>`: The username of the file sender.
- 

If the sender is found, the server responds to the recipient's acceptance with the next message, which indicates that the acceptance was successful:


```
S -> R (Receiver):  FILE_TRANSFER_ACCEPT_RESP {"status": "OK"}
```
 Status `OK` indicates the success of the receiver's acceptance.

**Rejection (Happy Flow)**

If the recipient rejects the file transfer, they specify the user from whom they want to reject the file transfer.
The next message is sent to the server.

```
R (Receiver) -> S:  FILE_TRANSFER_REJECT {"sender": "<sender>"}
```
- `<sender>`: The username of the file sender.

If the sender is found, the server responds to the recipient's rejection with the next message,
which indicates that the rejection was successful:


```
S -> R (Receiver):  FILE_TRANSFER_REJECT_RESP {"status": "<status>"}
```
Status `OK` indicates the success of the recipient's rejection.

After the successful acceptance or rejection from the receiver's side, 
the sender is notified about the receiver's decision.

```
S ->  S(Sender): FILE_TRANSFER_RESP {"status": "ERROR"}
```

```
S -> S(Sender): FILE_TRANSFER_RESP {"status": "OK" }
```

- `status`: Indicates the receiver's response to the file transfer request.
  `OK`: The receiver accepts the file transfer. 
  `ERROR`: The receiver rejects the file transfer

If the receiver rejects the file transfer,
the process ends at the moment when the sender is notified about the receiver's rejection.

If the receiver accepts the file transfer, the sender is notified with an `OK` response. 
Both the sender and receiver then receive a `FILE_TRANSFER_INIT` message,
which provides both parties with the UUID for further connection between them.

```
S ->  S (Sender): FILE_TRANSFER_INIT {"uuid": "<uuid>"}
```


```
S ->  R (Receiver): FILE_TRANSFER_INIT {"uuid": "<uuid>"}
```

`uuid`: Identifier for the file transfer session.

After receiving `FILE_TRANSFER_INIT` message, 
both users automatically open the connection to the file transfer server on port 1338.

```
S(Sender) -> S ( open the connection to the port 1338) 
```


```
R(Receiver) -> S (open the connection to the port 1338)
```


The sender sends bytes to the server, which include the `UUID`,
the `S` byte indicating the sender's role, and the file bytes.

```
S(Sender) -> S (<UUID>+S+file_bytes) 
```


The receiver sends bytes of the `UUID` and the `R` byte, which indicates the receiver's role.

```
R(Receiver) -> S (<UUID>+R)
```

After the file transfer server receives both parties' connections, 
it starts the actual exchange of file bytes between the sender stream and the receiver stream.



```
S (Sender) -> S : <file_bytes>
```


```
S ->  R (Receiver): <file_bytes>
```

The process ends when all bytes are transmitted to the receiver's side,
and the receiver's side performs a checksum for file integrity 
to inform the user whether the file was transmitted correctly or corrupted during transmission.

## 8.2 Unhappy flow

When the sender sends a request to the server,
the server will respond immediately with `FILE_TRANSFER_RESP` to inform the sender about the error, 
including an error status and code.


```
S ->  S(Sender): FILE_TRANSFER_RESP {"status": "ERROR", "code": <error code>}
```

Possible `<error code>`:

| Error code | Description                  |
|------------|------------------------------|
| 6000       | User sender is not logged in |
| 6004       | Receiver user not found      |


**Unhappy flow for acceptance:**


If the receiver specifies the sender of the file transfer request incorrectly during the acceptance,
or if the sender has left the server after sending the request,
the receiver will receive the following message from the server to indicate that the file transfer cannot be accepted.



```
S ->  R (Receiver):  FILE_TRANSFER_ACCEPT_RESP {"status": "ERROR", "code": <error code>}
```

Possible `<error code>`:

| Error code | Description                  |
|------------|------------------------------|
| 6004       | Sender user not found        |


**Unhappy flow for rejection:**

If the receiver specifies the sender of the file transfer request incorrectly during the rejection,
or if the sender has left the server after sending the request,
the receiver will receive the following message from the server to indicate that the file transfer cannot be rejected.



```
S ->  R (Receiver):  FILE_TRANSFER_REJECT_RESP {"status": "ERROR", "code": <error code>}
```

Possible `<error code>`:

| Error code | Description                  |
|------------|------------------------------|
| 6004       | Sender user not found        |




# 9. Invalid message header

If the client sends an invalid message header (not defined above), the server replies with an unknown command message. The client remains connected.

Example:
```
C -> S: MSG This is an invalid message
S -> C: UNKNOWN_COMMAND
```


# 10. Invalid message body

If the client sends a valid message, but the body is not valid JSON, the server replies with a pars error message. The client remains connected.

Example:
```
C -> S: BROADCAST_REQ {"aaaa}
S -> C: PARSE_ERROR
```
