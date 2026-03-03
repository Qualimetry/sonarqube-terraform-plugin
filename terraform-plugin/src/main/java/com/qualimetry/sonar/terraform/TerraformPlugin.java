package com.qualimetry.sonar.terraform;

import org.sonar.api.Plugin;

/**
 * Entry point for the Qualimetry Terraform SonarQube plugin.
 * Registers rules repository, quality profile, and sensor (Phase 3).
 */
public class TerraformPlugin implements Plugin {

    @Override
    public void define(Context context) {
        context.addExtensions(
                TerraformRulesDefinition.class,
                TerraformQualityProfile.class,
                TerraformSensor.class);
    }
}
