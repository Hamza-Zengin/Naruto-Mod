package com.sekwah.narutomod;

import com.sekwah.narutomod.client.model.entity.SusanooRibcageModel;
import com.sekwah.narutomod.client.model.item.model.AnbuMaskModel;
import com.sekwah.narutomod.client.model.item.model.HeadbandModel;
import com.sekwah.narutomod.client.model.item.model.KageHatModel;
import com.sekwah.narutomod.client.model.item.model.ShinobiArmorModel;
import com.sekwah.narutomod.client.model.jutsu.JutsuVisualModels;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Audit test preventing crashes such as NoSuchElementException from getChild calls on model parts.
 */
public class ModelPartsAuditTest {

    private static final Path SRC_MAIN_JAVA = Paths.get("src/main/java");
    private static final Pattern GET_CHILD_PATTERN = Pattern.compile("\\.getChild\\(\"([^\"]+)\"\\)");
    private static final Pattern ROOT_REPLACE_HEAD_PATTERN = Pattern.compile("root\\.addOrReplaceChild\\(\"head\"");

    @Test
    public void testAllGetChildCallsHaveMatchingAddOrReplaceChild() throws IOException {
        List<String> errors = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(SRC_MAIN_JAVA)) {
            stream.filter(p -> p.toString().endsWith(".java")).forEach(path -> {
                try {
                    String content = Files.readString(path);
                    List<String> lines = Files.readAllLines(path);
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        Matcher matcher = GET_CHILD_PATTERN.matcher(line);
                        while (matcher.find()) {
                            String part = matcher.group(1);
                            if (!content.contains("addOrReplaceChild(\"" + part + "\"")) {
                                errors.add(path.getFileName() + ":" + (i + 1) + " - ModelPart '" + part
                                        + "' called via getChild() but not defined with addOrReplaceChild()!");
                            }
                        }
                    }

                    if (content.contains("extends HumanoidModel")) {
                        boolean usesHumanoidMesh = content.contains("HumanoidModel.createMesh(")
                                || content.contains("ModelUtils.createBlankHumanoidMesh()");
                        if (!usesHumanoidMesh) {
                            errors.add(path.getFileName()
                                    + " extends HumanoidModel but does not build on HumanoidModel.createMesh() or ModelUtils.createBlankHumanoidMesh()");
                        }
                        if (ROOT_REPLACE_HEAD_PATTERN.matcher(content).find()
                                && !content.contains("addOrReplaceChild(\"hat\"")) {
                            errors.add(path.getFileName()
                                    + " replaces 'head' on root without re-adding 'hat' child under 'head'!");
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        Assertions.assertTrue(errors.isEmpty(),
                "Found " + errors.size() + " model part inconsistency errors:\n" + String.join("\n", errors));
    }

    @Test
    public void testHumanoidModelsBakeAndContainHeadHatHierarchy() {
        assertHumanoidModel(new SusanooRibcageModel(SusanooRibcageModel.createLayer().bakeRoot()),
                SusanooRibcageModel.createLayer().bakeRoot(), "SusanooRibcageModel");
        assertHumanoidModel(new AnbuMaskModel(AnbuMaskModel.createLayer(true).bakeRoot()),
                AnbuMaskModel.createLayer(true).bakeRoot(), "AnbuMaskModel(ears)");
        assertHumanoidModel(new AnbuMaskModel(AnbuMaskModel.createLayer(false).bakeRoot()),
                AnbuMaskModel.createLayer(false).bakeRoot(), "AnbuMaskModel(noEars)");
        assertHumanoidModel(new HeadbandModel(HeadbandModel.createLayer().bakeRoot()),
                HeadbandModel.createLayer().bakeRoot(), "HeadbandModel");
        assertHumanoidModel(new KageHatModel(KageHatModel.createLayer().bakeRoot()),
                KageHatModel.createLayer().bakeRoot(), "KageHatModel");
        assertHumanoidModel(new ShinobiArmorModel(ShinobiArmorModel.createChuninVestLayer().bakeRoot()),
                ShinobiArmorModel.createChuninVestLayer().bakeRoot(), "ShinobiArmorModel(chuninVest)");
        assertHumanoidModel(new ShinobiArmorModel(ShinobiArmorModel.createJoninVestLayer().bakeRoot()),
                ShinobiArmorModel.createJoninVestLayer().bakeRoot(), "ShinobiArmorModel(joninVest)");
        assertHumanoidModel(new ShinobiArmorModel(ShinobiArmorModel.createAnbuArmorLayer().bakeRoot()),
                ShinobiArmorModel.createAnbuArmorLayer().bakeRoot(), "ShinobiArmorModel(anbuArmor)");
        assertHumanoidModel(new ShinobiArmorModel(ShinobiArmorModel.createAkatsukiCloakLayer().bakeRoot()),
                ShinobiArmorModel.createAkatsukiCloakLayer().bakeRoot(), "ShinobiArmorModel(akatsukiCloak)");
        assertHumanoidModel(new ShinobiArmorModel(ShinobiArmorModel.createWarringStatesArmorLayer().bakeRoot()),
                ShinobiArmorModel.createWarringStatesArmorLayer().bakeRoot(), "ShinobiArmorModel(warringStatesArmor)");

        assertLegArmorModel(ShinobiArmorModel.createShinobiLeggingsLayer().bakeRoot(), "ShinobiArmorModel(shinobiLeggings)");
        assertLegArmorModel(ShinobiArmorModel.createShinobiSandalsLayer().bakeRoot(), "ShinobiArmorModel(shinobiSandals)");
        assertLegArmorModel(ShinobiArmorModel.createAnbuLeggingsLayer().bakeRoot(), "ShinobiArmorModel(anbuLeggings)");
        assertLegArmorModel(ShinobiArmorModel.createAnbuBootsLayer().bakeRoot(), "ShinobiArmorModel(anbuBoots)");
        assertLegArmorModel(ShinobiArmorModel.createWarringStatesLeggingsLayer().bakeRoot(), "ShinobiArmorModel(warringStatesLeggings)");
        assertLegArmorModel(ShinobiArmorModel.createWarringStatesBootsLayer().bakeRoot(), "ShinobiArmorModel(warringStatesBoots)");

        ModelPart chainsRoot = JutsuVisualModels.AdamantineChainsModel.createLayer().bakeRoot();
        Assertions.assertNotNull(new JutsuVisualModels.AdamantineChainsModel(chainsRoot),
                "AdamantineChainsModel should instantiate cleanly");
        Assertions.assertNotNull(chainsRoot.getChild("seal_ring"));
        Assertions.assertNotNull(chainsRoot.getChild("inner_seal_ring"));
        Assertions.assertNotNull(chainsRoot.getChild("chains_group"));

        ModelPart ashRoot = JutsuVisualModels.AshCloudModel.createLayer().bakeRoot();
        Assertions.assertNotNull(new JutsuVisualModels.AshCloudModel(ashRoot),
                "AshCloudModel should instantiate cleanly");
        Assertions.assertNotNull(ashRoot.getChild("core_plume"));
        Assertions.assertNotNull(ashRoot.getChild("swirl_ring"));
        Assertions.assertNotNull(ashRoot.getChild("ember_crown"));

        ModelPart waterWallRoot = JutsuVisualModels.WaterWallModel.createLayer().bakeRoot();
        Assertions.assertNotNull(new JutsuVisualModels.WaterWallModel(waterWallRoot),
                "WaterWallModel should instantiate cleanly");
        Assertions.assertNotNull(waterWallRoot.getChild("wave_base"));
        Assertions.assertNotNull(waterWallRoot.getChild("crest_surge"));
        Assertions.assertNotNull(waterWallRoot.getChild("foam_rings"));

        ModelPart scalpelRoot = JutsuVisualModels.ChakraScalpelModel.createLayer().bakeRoot();
        Assertions.assertNotNull(new JutsuVisualModels.ChakraScalpelModel(scalpelRoot),
                "ChakraScalpelModel should instantiate cleanly");
        Assertions.assertNotNull(scalpelRoot.getChild("scalpel_core"));
        Assertions.assertNotNull(scalpelRoot.getChild("edge_aura"));
    }

    private static void assertHumanoidModel(Object instance, ModelPart root, String label) {
        Assertions.assertNotNull(instance, label + " should instantiate cleanly");
        ModelPart head = root.getChild("head");
        Assertions.assertNotNull(head, label + " missing 'head' child");
        ModelPart hat = head.getChild("hat");
        Assertions.assertNotNull(hat, label + " missing 'hat' child under 'head'");
    }

    private static void assertLegArmorModel(ModelPart root, String label) {
        assertHumanoidModel(new ShinobiArmorModel(root), root, label);
        Assertions.assertNotNull(root.getChild("right_leg"), label + " missing 'right_leg' child");
        Assertions.assertNotNull(root.getChild("left_leg"), label + " missing 'left_leg' child");
    }
}

