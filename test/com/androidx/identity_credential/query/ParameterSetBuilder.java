package com.androidx.identity_credential.query;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.UnicodeString;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ParameterSetBuilder {
    private ParameterSet set = new ParameterSet();

    public ParameterSetBuilder add(String name, String value) {
        set.put(name, new UnicodeString(value));
        return this;
    }

    public ParameterSetBuilder add(String name, long value) {
        set.put(name, new CborBuilder().add(value).build().get(0));
        return this;
    }

    public ParameterSetBuilder add(String name, Date value) {
        set.put(name, buildTagged(ParameterSet.DATE_TAG,
                new SimpleDateFormat("yyyy-MM-dd").format(value)));
        return this;
    }

    public static DataItem buildTagged(int tag, String value) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CborEncoder encoder = new CborEncoder(baos);
            encoder.encode(new CborBuilder().addTag(tag).add(value).build());
            return new CborDecoder(new ByteArrayInputStream(baos.toByteArray())).decode().get(0);
        } catch (CborException e) {
            throw new RuntimeException(e);
        }
    }

    public static DataItem buildTagged(int tag, int value) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CborEncoder encoder = new CborEncoder(baos);
            encoder.encode(new CborBuilder().addTag(tag).add(value).build());
            return new CborDecoder(new ByteArrayInputStream(baos.toByteArray())).decode().get(0);
        } catch (CborException e) {
            throw new RuntimeException(e);
        }
    }

    public ParameterSet build() {
        return set;
    }
}
