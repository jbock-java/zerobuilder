package isobuilder.examples.kompliziert;

import javax.annotation.Generated;

@Generated("prototype")
public class PrototypeBobBuilder implements PrototypeBobBuilder_Contract.Modder, PrototypeBobBuilder_Contract.ChantalStep, PrototypeBobBuilder_Contract.JustinStep, PrototypeBobBuilder_Contract.KevinStep {

    private String kevin;
    private String chantal;
    private String justin;

    static PrototypeBobBuilder_Contract.KevinStep builder() {
        return new PrototypeBobBuilder();
    }

    @Override
    public PrototypeBobBuilder_Contract.ChantalStep kevin(String kevin) {
        this.kevin = kevin;
        return this;
    }

    @Override
    public PrototypeBobBuilder_Contract.JustinStep chantal(String chantal) {
        this.chantal = chantal;
        return this;
    }

    @Override
    public PrototypeBobBuilder_Contract.Modder justin(String justin) {
        this.justin = justin;
        return this;
    }

    @Override
    public PrototypeBobBuilder_Contract.Modder updateKevin(String kevin) {
        this.kevin = kevin;
        return this;
    }

    @Override
    public PrototypeBobBuilder_Contract.Modder updateChantal(String chantal) {
        this.chantal = chantal;
        return this;
    }

    @Override
    public PrototypeBobBuilder_Contract.Modder updateJustin(String justin) {
        this.justin = justin;
        return this;
    }

    @Override
    public Bob build() {
        return Bob.createBob(kevin, chantal, justin);
    }

}
