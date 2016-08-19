package isobuilder.examples.kompliziert;

public class Bob {

    private final String kevin;
    private final String chantal;
    private final String justin;

    private Bob(String kevin, String chantal, String justin) {
        this.kevin = kevin;
        this.chantal = chantal;
        this.justin = justin;
    }

//    @Builder
    static Bob createBob(String kevin, String chantal, String justin) {
        return new Bob(kevin, chantal, justin);
    }

    @Override
    public String toString() {
        return "Bob{" +
                "kevin='" + kevin + '\'' +
                ", chantal='" + chantal + '\'' +
                ", justin='" + justin + '\'' +
                '}';
    }
}
