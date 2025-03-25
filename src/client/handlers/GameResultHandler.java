package client.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.GameResult;
import shared.utility.GameMoves;

import java.io.IOException;
import java.util.function.Consumer;

public class GameResultHandler implements Consumer<String> {

    private final ObjectMapper mapper;

    public GameResultHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void accept(String payload) {
        try {
            GameResult gameResult = mapper.readValue(payload, GameResult.class);
            String winner = gameResult.winner();
            String initiatorMove = gameResult.initiatorMove();
            String opponentMove = gameResult.opponentMove();
            String initiatorMoveName = GameMoves.fromCode(initiatorMove).getName();
            String opponentMoveName = GameMoves.fromCode(opponentMove).getName();
            if(winner==null) {
                System.out.println("Game result: draw. Initiator move: " + initiatorMoveName+ ", opponent move: " + opponentMoveName);
            }else{
                System.out.println("Game winner: "+winner+". Initiator move: " + initiatorMoveName+ ", opponent move: " + opponentMoveName);
            }

        } catch (IOException e) {
            System.err.println("Failed to process game result message: " + e.getMessage());
        }
    }
}
