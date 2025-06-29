package backend.synGo.filesystem;

import java.util.Arrays;

public enum AllowedImageExtension {
    JPG, JPEG, PNG, BMP, WEBP, SVG, AI, AVIF;

    public static boolean isNotContain(final String imageExtension) {
        final String upperCase = imageExtension.toUpperCase();

        return Arrays.stream(values())
                .noneMatch(allowedImageExtension -> allowedImageExtension.name().equals(upperCase));
    }

}
