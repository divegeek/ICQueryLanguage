package com.androidx.identity_credential.query;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.SimpleValue;
import co.nstant.in.cbor.model.UnsignedInteger;
import org.junit.jupiter.api.Test;

import java.util.GregorianCalendar;

import static com.androidx.identity_credential.query.ICQueryExecutor.*;
import static com.androidx.identity_credential.query.ParameterSet.DATE_TAG;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class ICQueryExecutorTest {

    private ICQueryExecutor mExecutor = new ICQueryExecutor();

    @Test
    void emptyQuery() {
        try {
            mExecutor.execute(new Array(), null, null);
            fail();
        } catch (QueryException e) {
            assertThat(e.getMessage(), containsString("Invalid query"));
        }
    }

    @Test
    void invalidQueryEntry() {
        Array entry = createParameterRef(TYPE_BOOLEAN, "invalid");
        entry.setTag(1000 /* invalid value */);

        Array query = new Array();
        query.add(entry);

        try {
            mExecutor.execute(query, null, null);
            fail();
        } catch (QueryException e) {
            assertThat(e.getMessage(), containsString("invalid tag"));
        }

        entry.removeTag();
        try {
            mExecutor.execute(query, null, null);
            fail();
        } catch (QueryException e) {
            assertThat(e.getMessage(), containsString("missing tag"));
        }
    }

    @Test
    void returnBooleanParamTest() throws QueryException {
        Array query = new Array();
        query.add(createParameterRef(TYPE_BOOLEAN, "a"));

        ParameterSet paramsSetTrue = new ParameterSet();
        paramsSetTrue.put("a", SimpleValue.TRUE);

        ParameterSet paramsSetFalse = new ParameterSet();
        paramsSetFalse.put("a", SimpleValue.FALSE);

        assertTrue(mExecutor.execute(query, paramsSetTrue, null));
        assertFalse(mExecutor.execute(query, paramsSetFalse, null));
    }

    @Test
    void returnNegatedBooleanParamTest() throws QueryException {
        Array query = new Array();
        query.add(createParameterRef(TYPE_BOOLEAN, "a"));
        query.add(createOperator(UNARY_NOT));

        ParameterSet paramsSetTrue = new ParameterSet();
        paramsSetTrue.put("a", SimpleValue.TRUE);

        ParameterSet paramsSetFalse = new ParameterSet();
        paramsSetFalse.put("a", SimpleValue.FALSE);

        assertFalse(mExecutor.execute(query, paramsSetTrue, null));
        assertTrue(mExecutor.execute(query, paramsSetFalse, null));
    }

    @Test
    void andTest() throws QueryException {
        checkBinaryTruthTable(AND, true, false, false, false);
    }

    @Test
    void orTest() throws QueryException {
        checkBinaryTruthTable(OR, true, true, true, false);
    }

    @Test
    void invalidStringOperators() {
        tryInvalidStringOperator(AND);
        tryInvalidStringOperator(OR);
        tryInvalidStringOperator(GREATER_OR_EQUAL);
        tryInvalidStringOperator(GREATER_THAN);
        tryInvalidStringOperator(LESS_OR_EQUAL);
        tryInvalidStringOperator(LESS_THAN);
    }

    @Test
    void stringEquality() throws QueryException {
        Array equalsQuery = createBinaryOperatorQuery(TYPE_STRING, EQUAL);
        Array notEqualsQuery = createBinaryOperatorQuery(TYPE_STRING, NOT_EQUAL);

        ParameterSet equalParams =
                new ParameterSetBuilder().add("a", "hello").add("b", "hello").build();
        ParameterSet notEqualParams =
                new ParameterSetBuilder().add("a", "hello").add("b", "bob").build();

        assertTrue(mExecutor.execute(equalsQuery, equalParams, null));
        assertFalse(mExecutor.execute(notEqualsQuery, equalParams, null));
        assertFalse(mExecutor.execute(equalsQuery, notEqualParams, null));
        assertTrue(mExecutor.execute(notEqualsQuery, notEqualParams, null));
    }

    @Test
    void numericComparisons() throws QueryException {
        ParameterSet equalParams = new ParameterSetBuilder().add("a", 1).add("b", 1).build();
        ParameterSet unequalParams = new ParameterSetBuilder().add("a", -1).add("b", 1).build();

        Array equalsQuery = createBinaryOperatorQuery(TYPE_INTEGER, EQUAL);
        assertTrue(mExecutor.execute(equalsQuery, equalParams, null));
        assertFalse(mExecutor.execute(equalsQuery, unequalParams, null));

        Array notEqualsQuery = createBinaryOperatorQuery(TYPE_INTEGER, NOT_EQUAL);
        assertFalse(mExecutor.execute(notEqualsQuery, equalParams, null));
        assertTrue(mExecutor.execute(notEqualsQuery, unequalParams, null));

        Array lessQuery = createBinaryOperatorQuery(TYPE_INTEGER, LESS_THAN);
        assertFalse(mExecutor.execute(lessQuery, equalParams, null));
        assertTrue(mExecutor.execute(lessQuery, unequalParams, null));

        Array lessEqualQuery = createBinaryOperatorQuery(TYPE_INTEGER, LESS_OR_EQUAL);
        assertTrue(mExecutor.execute(lessEqualQuery, equalParams, null));
        assertTrue(mExecutor.execute(lessEqualQuery, unequalParams, null));

        Array greaterQuery = createBinaryOperatorQuery(TYPE_INTEGER, GREATER_THAN);
        assertFalse(mExecutor.execute(greaterQuery, equalParams, null));
        assertFalse(mExecutor.execute(greaterQuery, unequalParams, null));

        Array greaterEqualQuery = createBinaryOperatorQuery(TYPE_INTEGER, GREATER_OR_EQUAL);
        assertTrue(mExecutor.execute(greaterEqualQuery, equalParams, null));
        assertFalse(mExecutor.execute(greaterEqualQuery, unequalParams, null));
    }

    @Test
    void compareDates() throws QueryException {
        ParameterSet equalParams =
                new ParameterSetBuilder().add("a", new GregorianCalendar(2019, 12, 10).getTime())
                                         .add("b", new GregorianCalendar(2019, 12, 10).getTime())
                                         .build();
        ParameterSet greaterParams =
                new ParameterSetBuilder().add("a", new GregorianCalendar(2019, 12, 10).getTime())
                                         .add("b", new GregorianCalendar(1998, 12, 10).getTime())
                                         .build();
        ParameterSet lessParams =
                new ParameterSetBuilder().add("a", new GregorianCalendar(1998, 12, 10).getTime())
                                         .add("b", new GregorianCalendar(2019, 12, 10).getTime())
                                         .build();

        Array equalsQuery = createBinaryOperatorQuery(DATE_TAG, TYPE_STRING, EQUAL);
        assertTrue(mExecutor.execute(equalsQuery, equalParams, null));
        assertFalse(mExecutor.execute(equalsQuery, lessParams, null));
        assertFalse(mExecutor.execute(equalsQuery, greaterParams, null));

        Array lessThanQuery = createBinaryOperatorQuery(DATE_TAG, TYPE_STRING, LESS_THAN);
        assertFalse(mExecutor.execute(lessThanQuery, equalParams, null));
        assertTrue(mExecutor.execute(lessThanQuery, lessParams, null));
        assertFalse(mExecutor.execute(lessThanQuery, greaterParams, null));

        Array greaterThanQuery = createBinaryOperatorQuery(DATE_TAG, TYPE_STRING, GREATER_THAN);
        assertFalse(mExecutor.execute(greaterThanQuery, equalParams, null));
        assertFalse(mExecutor.execute(greaterThanQuery, lessParams, null));
        assertTrue(mExecutor.execute(greaterThanQuery, greaterParams, null));
    }

    private Array createBinaryOperatorQuery(int type, int op) {
        Array query = new Array();
        query.add(createParameterRef(type, "a"));
        query.add(createParameterRef(type, "b"));
        query.add(createOperator(op));
        return query;
    }

    private Array createBinaryOperatorQuery(int tag, int type, int op) {
        Array query = new Array();
        query.add(createParameterRef(tag, type, "a"));
        query.add(createParameterRef(tag, type, "b"));
        query.add(createOperator(tag, op));
        return query;
    }

    private void tryInvalidStringOperator(int operator) {
        Array query = createBinaryOperatorQuery(TYPE_STRING, operator);

        ParameterSet params = new ParameterSetBuilder().add("a", "hello").add("b", "bob").build();

        try {
            mExecutor.execute(query, params, null);
            fail();
        } catch (QueryException e) {
            assertThat(e.getMessage(), containsString("Invalid query"));
        }
    }

    private void checkBinaryTruthTable(int binaryOperator, boolean trueTrue, boolean trueFalse,
                                       boolean falseTrue, boolean falseFalse) throws QueryException {
        Array query = createBinaryOperatorQuery(TYPE_BOOLEAN, binaryOperator);

        ParameterSet paramsTrueTrue = new ParameterSet();
        paramsTrueTrue.put("a", SimpleValue.TRUE);
        paramsTrueTrue.put("b", SimpleValue.TRUE);

        ParameterSet paramsTrueFalse = new ParameterSet();
        paramsTrueFalse.put("a", SimpleValue.TRUE);
        paramsTrueFalse.put("b", SimpleValue.FALSE);

        ParameterSet paramsFalseTrue = new ParameterSet();
        paramsFalseTrue.put("a", SimpleValue.FALSE);
        paramsFalseTrue.put("b", SimpleValue.TRUE);

        ParameterSet paramsFalseFalse = new ParameterSet();
        paramsFalseFalse.put("a", SimpleValue.FALSE);
        paramsFalseFalse.put("b", SimpleValue.FALSE);

        assertEquals(trueTrue, mExecutor.execute(query, paramsTrueTrue, null));
        assertEquals(trueFalse, mExecutor.execute(query, paramsTrueFalse, null));
        assertEquals(falseTrue, mExecutor.execute(query, paramsFalseTrue, null));
        assertEquals(falseFalse, mExecutor.execute(query, paramsFalseFalse, null));
    }

    private UnsignedInteger createOperator(int op) {
        UnsignedInteger operator = new UnsignedInteger(op);
        operator.setTag(OPERATOR);
        return operator;
    }

    private DataItem createOperator(int tag, int op) {
        return new CborBuilder().add(op).tagged(tag).tagged(OPERATOR).build().get(0);
    }


    private Array createParameterRef(int type, String name) {
        Array paramRef =
                (Array) new CborBuilder().addArray().add(name).add(type).end().build().get(0);
        paramRef.setTag(PARAM_REF);
        return paramRef;
    }

    private Array createParameterRef(int tag, int type, String name) {
        Array paramRef = (Array) new CborBuilder().addArray().add(name).add(type).tagged(tag)
                                                  .end().build().get(0);
        paramRef.setTag(PARAM_REF);
        return paramRef;
    }
}
