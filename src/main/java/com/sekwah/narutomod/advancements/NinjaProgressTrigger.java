package com.sekwah.narutomod.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sekwah.narutomod.registries.NarutoRegistries;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

/**
 * Custom SimpleCriterionTrigger tracking core Shinobi progression milestones:
 * clan selection, jutsu usage (including water walking), elemental combos, and rank promotions.
 */
public class NinjaProgressTrigger extends SimpleCriterionTrigger<NinjaProgressTrigger.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, Action action) {
        this.trigger(player, action, null);
    }

    public void trigger(ServerPlayer player, Action action, @Nullable String target) {
        this.trigger(player, instance -> instance.matches(action, target));
    }

    public enum Action implements StringRepresentable {
        SELECT_CLAN("select_clan"),
        USE_JUTSU("use_jutsu"),
        ELEMENTAL_COMBO("elemental_combo"),
        RANK_UP("rank_up");

        public static final Codec<Action> CODEC = StringRepresentable.fromEnum(Action::values);

        private final String serializedName;

        Action(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return this.serializedName;
        }
    }

    public record Instance(
            Optional<ContextAwarePredicate> player,
            Action action,
            Optional<String> target
    ) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Action.CODEC.fieldOf("action").forGetter(Instance::action),
                Codec.STRING.optionalFieldOf("target").forGetter(Instance::target)
        ).apply(instance, Instance::new));

        public static Criterion<Instance> of(Action action) {
            return NarutoRegistries.NINJA_PROGRESS_TRIGGER.get()
                    .createCriterion(new Instance(Optional.empty(), action, Optional.empty()));
        }

        public static Criterion<Instance> of(Action action, String target) {
            return NarutoRegistries.NINJA_PROGRESS_TRIGGER.get()
                    .createCriterion(new Instance(Optional.empty(), action, Optional.of(target)));
        }

        public boolean matches(Action firedAction, @Nullable String firedTarget) {
            if (this.action != firedAction) {
                return false;
            }
            if (this.target.isEmpty()) {
                return true;
            }
            if (firedTarget == null) {
                return false;
            }
            return this.target.get().toLowerCase(Locale.ROOT)
                    .equals(firedTarget.toLowerCase(Locale.ROOT));
        }
    }
}
