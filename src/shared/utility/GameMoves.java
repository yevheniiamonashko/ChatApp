package shared.utility;


public enum GameMoves {
    R("R",  "Rock"), // Rock
    P("P", "Paper"), // Paper
    S("S", "Scissors"); // Scissors

    private final String code;
    private final String name;

    GameMoves(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }
    public String getName() {
        return name;
    }

    // Static method to retrieve the GameMoves enum from a code
    public static GameMoves fromCode(String code) {
        for (GameMoves gameMove : GameMoves.values()) {
            if (gameMove.getCode().equalsIgnoreCase(code)) {
                return gameMove;
            }
        }
        return null;
    }
}
