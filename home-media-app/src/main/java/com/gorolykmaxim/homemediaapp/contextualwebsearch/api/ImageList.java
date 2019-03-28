package com.gorolykmaxim.homemediaapp.contextualwebsearch.api;

import java.util.List;
import java.util.Objects;

public class ImageList {
    private List<Image> images;

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageList imageList = (ImageList) o;
        return Objects.equals(images, imageList.images);
    }

    @Override
    public int hashCode() {
        return Objects.hash(images);
    }
}
