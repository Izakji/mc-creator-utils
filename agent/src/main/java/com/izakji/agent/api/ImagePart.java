package com.izakji.agent.api;

import java.util.Objects;

/**
 * Binary image content.
 *
 * @param mediaType MIME type
 * @param data image bytes
 * @author Izakji
 */
public record ImagePart(String mediaType, byte[] data) implements ContentPart {
    public ImagePart {
        mediaType = Objects.requireNonNull(mediaType, "mediaType");
        data = Objects.requireNonNull(data, "data").clone();
    }

    @Override
    public byte[] data() {
        return data.clone();
    }

    @Override
    public String type() {
        return "image";
    }
}
