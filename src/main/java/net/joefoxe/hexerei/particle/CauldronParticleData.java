package net.joefoxe.hexerei.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Locale;

/**
 * Created by TGG on 25/03/2020.
 * <p>
 * The particle has two pieces of information which are used to customise it:
 * <p>
 * 1) The colour (tint) which is used to change the hue of the particle
 * 2) The diameter of the particle
 * <p>
 * This class is used to
 * 1) store this information, and
 * 2) transmit it between server and client (write and read methods), and
 * 3) parse it from a command string i.e. the /particle params
 */
public class CauldronParticleData implements ParticleOptions {

    public CauldronParticleData(Color tint, double diameter) {
        this.tint = tint;
        this.diameter = constrainDiameterToValidRange(diameter);
    }

    public Color getTint() {
        return tint;
    }

    public double getDiameter() {
        return diameter;
    }

    @Nonnull
    @Override
    public ParticleType<CauldronParticleData> getType() {
        return ModParticleTypes.cauldronParticleType;
    }

    // write the particle information to a FriendlyByteBuf, ready for transmission to a client
    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeInt(tint.getRed());
        buf.writeInt(tint.getGreen());
        buf.writeInt(tint.getBlue());
        buf.writeDouble(diameter);
    }

    // used for debugging I think; prints the data in human-readable format
    @Nonnull
    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %.2f %i %i %i",
                this.getType(), diameter, tint.getRed(), tint.getGreen(), tint.getBlue());
    }

    private static double constrainDiameterToValidRange(double diameter) {
        final double MIN_DIAMETER = 0.05;
        final double MAX_DIAMETER = 1.0;
        return Mth.clamp(diameter, MIN_DIAMETER, MAX_DIAMETER);
    }

    private Color tint;
    private double diameter;

    // --------- these remaining methods are used to serialize the Particle Data.
    //  I'm not yet sure what the Codec is used for, given that the DESERIALIZER already deserializes using read.
    //  Perhaps it will be used to replace the manual read methods in the future.

    //  The CODEC is a convenience to make it much easier to serialise and deserialize your objects.
    //  Using the builder below, you construct a serializer and deserializer in one go, using lambda functions.
    //  eg for the FlameParticleData CODEC:
    //  a) In order to serialise it, it reads the 'tint' member variable (type: INT) and the 'diameter' member variable (type: DOUBLE)
    //  b) In order to deserialise it, call the matching constructor FlameParticleData(INT, DOUBLE)

    public static final Codec<CauldronParticleData> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.INT.fieldOf("tint").forGetter(d -> d.tint.getRGB()),
                    Codec.DOUBLE.fieldOf("diameter").forGetter(d -> d.diameter)
            ).apply(instance, CauldronParticleData::new)
    );

    private CauldronParticleData(int tintRGB, double diameter) {
        this.tint = new Color(tintRGB);
        this.diameter = constrainDiameterToValidRange(diameter);
    }

    // The DESERIALIZER is used to construct CauldronParticleData from either command line parameters or from a network packet

    public static final Deserializer<CauldronParticleData> DESERIALIZER = new Deserializer<>() {

        // parse the parameters for this particle from a /particle command
        @Nonnull
        @Override
        public CauldronParticleData fromCommand(@Nonnull ParticleType<CauldronParticleData> type, @Nonnull StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            double diameter = constrainDiameterToValidRange(reader.readDouble());

            final int MIN_COLOUR = 0;
            final int MAX_COLOUR = 255;
            reader.expect(' ');
            int red = Mth.clamp(reader.readInt(), MIN_COLOUR, MAX_COLOUR);
            reader.expect(' ');
            int green = Mth.clamp(reader.readInt(), MIN_COLOUR, MAX_COLOUR);
            reader.expect(' ');
            int blue = Mth.clamp(reader.readInt(), MIN_COLOUR, MAX_COLOUR);
            Color color = new Color(red, green, blue);

            return new CauldronParticleData(color, diameter);
        }

        // read the particle information from a FriendlyByteBuf after the client has received it from the server
        @Override
        public CauldronParticleData fromNetwork(@Nonnull ParticleType<CauldronParticleData> type, FriendlyByteBuf buf) {
            // warning! never trust the data read in from a packet buffer.

            final int MIN_COLOUR = 0;
            final int MAX_COLOUR = 255;
            int red = Mth.clamp(buf.readInt(), MIN_COLOUR, MAX_COLOUR);
            int green = Mth.clamp(buf.readInt(), MIN_COLOUR, MAX_COLOUR);
            int blue = Mth.clamp(buf.readInt(), MIN_COLOUR, MAX_COLOUR);
            Color color = new Color(red, green, blue);

            double diameter = constrainDiameterToValidRange(buf.readDouble());

            return new CauldronParticleData(color, diameter);
        }
    };
}
