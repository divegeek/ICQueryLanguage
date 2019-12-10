package com.androidx.identity_credential.query;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.model.UnicodeString;

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

    public ParameterSet build() {
        return set;
    }
}
