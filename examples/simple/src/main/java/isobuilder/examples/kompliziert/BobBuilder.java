package isobuilder.examples.kompliziert;

import javax.annotation.Generated;

@Generated("von Hand")
public class BobBuilder implements BobBuilder_Contract.Modder, BobBuilder_Contract.ChantalStep, BobBuilder_Contract.JustinStep, BobBuilder_Contract.KevinStep {

    private String kevin;
    private String chantal;
    private String justin;

    static BobBuilder_Contract.KevinStep builder() {
        return new BobBuilder();
    }

    @Override
    public BobBuilder_Contract.ChantalStep kevin(String kevin) {
        this.kevin = kevin;
        return this;
    }

    @Override
    public BobBuilder_Contract.JustinStep chantal(String chantal) {
        this.chantal = chantal;
        return this;
    }

    @Override
    public BobBuilder_Contract.Modder justin(String justin) {
        this.justin = justin;
        return this;
    }

    @Override
    public BobBuilder_Contract.Modder updateKevin(String kevin) {
        this.kevin = kevin;
        return this;
    }

    @Override
    public BobBuilder_Contract.Modder updateChantal(String chantal) {
        this.chantal = chantal;
        return this;
    }

    @Override
    public BobBuilder_Contract.Modder updateJustin(String justin) {
        this.justin = justin;
        return this;
    }

    @Override
    public Bob build() {
        return Bob.createBob(kevin, chantal, justin);
    }

}
