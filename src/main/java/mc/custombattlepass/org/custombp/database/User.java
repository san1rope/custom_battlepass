package mc.custombattlepass.org.custombp.database;

public class User {
    private String nickname;
    private String playeruuid;

    public User(String nickname, String playeruuid) {
        this.nickname = nickname;
        this.playeruuid = playeruuid;
    }

    public User() {
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPlayeruuid() {
        return playeruuid;
    }

    public void setPlayeruuid(String playeruuid) {
        this.playeruuid = playeruuid;
    }
}
