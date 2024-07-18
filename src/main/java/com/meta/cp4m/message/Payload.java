/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface Payload<T> {

  T value();

  int size();

  final class Text implements Payload<String> {

    private final String payload;

    public Text(String payload) {
      this.payload = payload;
    }

    @Override
    public String value() {
      return this.payload;
    }

    @Override
    public int size() {
      return payload.length();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Text text = (Text) o;
      return Objects.equals(payload, text.payload);
    }

    @Override
    public int hashCode() {
      return Objects.hash(payload);
    }
  }

  final class Image implements Payload<byte[]> {
    private final String mimeType;
    private final String extension;
    private final byte[] payload;

    public Image(byte[] payload, String mimeType) {
      System.out.println("Image constructor");
      this.extension = MimeTypeUtils.getMimeToExt(mimeType.strip());
      this.mimeType = mimeType;
      this.payload = payload;
    }

    @Override
    public byte[] value() {
      return payload;
    }

    @Override
    public int size() {
      return payload.length;
    }

    public String extension() {
      return this.extension;
    }

    public String mimeType() {
      return mimeType;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Image image = (Image) o;
      return Objects.equals(mimeType, image.mimeType)
          && Objects.equals(extension, image.extension)
          && Arrays.equals(payload, image.payload);
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(mimeType, extension);
      result = 31 * result + Arrays.hashCode(payload);
      return result;
    }
  }

  final class Document implements Payload<byte[]> {
    private final String mimeType;
    private final String extension;
    private final byte[] payload;

    public Document(byte[] payload, String mimeType) {
      this.extension = MimeTypeUtils.getMimeToExt(mimeType.strip());
      this.mimeType = mimeType;
      this.payload = payload;
    }

    @Override
    public byte[] value() {
      return payload;
    }

    @Override
    public int size() {
      return payload.length;
    }

    public String extension() {
      return this.extension;
    }

    public String mimeType() {
      return mimeType;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Document document = (Document) o;
      return Objects.equals(mimeType, document.mimeType)
          && Objects.equals(extension, document.extension)
          && Arrays.equals(payload, document.payload);
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(mimeType, extension);
      result = 31 * result + Arrays.hashCode(payload);
      return result;
    }
  }

  final class MimeTypeUtils {
    // MIME types and corresponding extensions
    private static final Map<String, String> MIME_TO_EXTENSION =
      ImmutableMap.<String, String>builder()
      .put("application/gzip", "gz")
      .put("application/json", "json")
      .put("application/msword", "doc")
      .put("application/pdf", "pdf")
      .put("application/rtf", "rtf")
      .put("application/vnd.amazon.ebook", "azw")
      .put("application/vnd.ms-excel", "xls")
      .put("application/vnd.ms-powerpoint", "ppt")
      .put("application/vnd.oasis.opendocument.presentation", "odp")
      .put("application/vnd.oasis.opendocument.spreadsheet", "ods")
      .put("application/vnd.oasis.opendocument.text", "odt")
      .put("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx")
      .put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx")
      .put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx")
      .put("application/vnd.rar", "rar")
      .put("application/x-abiword", "abw")
      .put("application/x-bzip", "bz")
      .put("application/x-bzip2", "bz2")
      .put("application/x-freearc", "arc")
      .put("application/x-tar", "tar")
      .put("application/xml", "xml")
      .put("application/zip", "zip")
      .put("image/apng", "apng")
      .put("image/avif", "avif")
      .put("image/bmp", "bmp")
      .put("image/gif", "gif")
      .put("image/jpeg", "jpeg")
      .put("image/png", "png")
      .put("image/svg+xml", "svg")
      .put("image/tiff", "tif")
      .put("image/webp", "webp")
      .put("text/calendar", "ics")
      .put("text/csv", "csv")
      .put("text/html", "html")
      .put("text/plain", "txt")
      .build();

    // Static method to get the file extension based on MIME type
    public static String getMimeToExt(String mimeType) {
      return Objects.requireNonNullElse(MIME_TO_EXTENSION.get(mimeType), "bin");
    }
  }
}
