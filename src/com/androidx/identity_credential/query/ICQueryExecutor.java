package com.androidx.identity_credential.query;

import co.nstant.in.cbor.model.Number;
import co.nstant.in.cbor.model.*;

import java.util.Deque;
import java.util.LinkedList;

import static com.androidx.identity_credential.query.ParameterSet.DATE_TAG;

public class ICQueryExecutor {

    public static final int DATA_REF = 300;
    public static final int PARAM_REF = 301;
    public static final int OPERATOR = 302;

    public static final int TYPE_INTEGER = 0;
    public static final int TYPE_STRING = 1;
    public static final int TYPE_BOOLEAN = 2;

    public static final int LESS_THAN = 0;
    public static final int LESS_OR_EQUAL = 1;
    public static final int EQUAL = 2;
    public static final int NOT_EQUAL = 3;
    public static final int GREATER_THAN = 4;
    public static final int GREATER_OR_EQUAL = 5;
    public static final int AND = 6;
    public static final int OR = 7;
    public static final int UNARY_NOT = 8;

    private final Deque<DataItem> stack = new LinkedList<>();

    public boolean execute(Array query, ParameterSet parameters, DataSet dataElements)
            throws QueryException {
        stack.clear();

        for (DataItem entry : query.getDataItems()) {
            Tag tag = entry.getTag();
            if (tag == null) {
                throw new QueryException("Invalid query entry: missing tag.");
            }

            // Operators can have both a tag on their type (indicating the required operand type)
            // as well as the tag specifying that the entry is an operator.  Get the outermost
            // tag in this case.
            if (tag.getTag() != null) {
                tag = tag.getTag();
            }

            DataItem newStackEntry;
            switch ((int) tag.getValue()) {
                case DATA_REF:
                    newStackEntry = dataElements.getDataElement(entry);
                    break;

                case PARAM_REF:
                    newStackEntry = parameters.getParameter(entry);
                    break;

                case OPERATOR:
                    newStackEntry = applyOperator(entry);
                    break;

                default:
                    throw new QueryException("Invalid query entry: invalid tag.");
            }

            stack.push(newStackEntry);
        }

        if (stack.size() != 1) {
            throw new QueryException(
                    ("Invalid query: " + stack.size() + " stack elements " + "remaining"));
        }
        if (stack.peek() instanceof SimpleValue) {
            SimpleValue result = (SimpleValue) stack.pop();
            if (result.getSimpleValueType().equals(SimpleValueType.TRUE)) {
                return true;
            }
            if (result.getSimpleValueType().equals(SimpleValueType.FALSE)) {
                return false;
            }
        }
        throw new QueryException(("Invalid query:  result is not boolean"));
    }

    private DataItem applyOperator(DataItem operator) throws QueryException {
        if (!(operator instanceof Number)) {
            throw new QueryException("Invalid query: non-integer operator");
        }
        Number op = (Number) operator;

        if (op.getValue().intValue() == UNARY_NOT) {
            return handleUnaryOperator(op);
        }
        return handleBinaryOperator(op);
    }

    private DataItem handleBinaryOperator(Number op) throws QueryException {
        DataItem operandB = stack.pop();
        DataItem operandA = stack.pop();

        if (!compareTags(operandA.getTag(), operandB.getTag())) {
            throw new QueryException(
                    "Invalid query:  Operands have different tags: " + operandA.getTag() + " and " +
                    operandB.getTag());
        }
        if (operandA.getTag() != null && !compareTags(op.getTag(), operandA.getTag())) {
            throw new QueryException(
                    "Invalid query:  Operands and operator have different type " + "tags: " +
                    op.getTag() + " and " + operandA.getTag());
        }

        boolean result = false;
        if (operandA instanceof Number) {
            if (!(operandB instanceof Number)) {
                throw new QueryException(
                        "Invalid query:  Operands are different types: " + operandA + " and " +
                        operandB);
            }
            result = operateOnNumbers(op, (Number) operandA, (Number) operandB);
        } else if (isBoolean(operandA)) {
            if (!isBoolean(operandB)) {
                throw new QueryException(
                        "Invalid query:  Operands are different types: " + operandA + " and " +
                        operandB);
            }
            result = operateOnBooleans(op, operandA, operandB);
        } else if (operandA instanceof UnicodeString) {
            if (!(operandB instanceof UnicodeString)) {
                throw new QueryException(
                        "Invalid query:  Operands are different types: " + operandA + " and " +
                        operandB);
            }
            result = operateOnStrings(op, (UnicodeString) operandA, (UnicodeString) operandB);
        } else {
            throw new QueryException("Invalid query:  Unsupported operand type");
        }
        return result ? SimpleValue.TRUE : SimpleValue.FALSE;
    }

    private SimpleValue handleUnaryOperator(Number op) throws QueryException {
        switch (op.getValue().intValue()) {
            case UNARY_NOT: {
                SimpleValue result;
                DataItem operand = stack.pop();
                if (!isBoolean(operand)) {
                    throw new QueryException(
                            "Invalid query:  Applying unary not to non-boolean " + "operand");
                }
                if (operand.equals(SimpleValue.TRUE)) {
                    result = SimpleValue.FALSE;
                } else {
                    result = SimpleValue.TRUE;
                }
                return result;
            }

            default:
                throw new QueryException("Invalid query:  Unknown unary operator");
        }
    }

    private boolean operateOnStrings(Number op, UnicodeString operandA, UnicodeString operandB)
            throws QueryException {
        if (operandA.getTag() != null && operandA.getTag().getValue() == DATE_TAG) {
            return evaluateOrderingOperation(op,
                    operandA.getString().compareTo(operandB.getString()));
        }

        boolean comparisonResult = operandA.equals(operandB);
        switch (op.getValue().intValue()) {
            case EQUAL:
                return comparisonResult;

            case NOT_EQUAL:
                return !comparisonResult;

            default:
                throw new QueryException("Invalid query:  Non-equality operator applied to others");
        }
    }

    private boolean operateOnBooleans(Number op, DataItem operandA, DataItem operandB)
            throws QueryException {
        switch (op.getValue().intValue()) {
            case AND:
                return toBoolean(operandA) && toBoolean(operandB);

            case OR:
                return toBoolean(operandA) || toBoolean(operandB);

            default:
                throw new QueryException(
                        "Invalid query:  Non-boolean operator applied to " + "booleans");
        }
    }

    private boolean operateOnNumbers(Number op, Number numberA, Number numberB)
            throws QueryException {
        return evaluateOrderingOperation(op, numberA.getValue().compareTo(numberB.getValue()));
    }

    private boolean evaluateOrderingOperation(Number op, int ordering) throws QueryException {
        switch (op.getValue().intValue()) {
            case LESS_THAN:
                return (ordering < 0);

            case LESS_OR_EQUAL:
                return (ordering <= 0);

            case EQUAL:
                return (ordering == 0);

            case NOT_EQUAL:
                return (ordering != 0);

            case GREATER_THAN:
                return (ordering > 0);

            case GREATER_OR_EQUAL:
                return (ordering >= 0);

            default:
                throw new QueryException("Invalid query:  Non-numeric operator applied to numbers");
        }
    }

    private boolean isBoolean(DataItem value) {
        if (!(value instanceof SimpleValue)) {
            return false;
        }
        SimpleValue simpleValue = (SimpleValue) value;
        return (simpleValue.equals(SimpleValue.TRUE) || simpleValue.equals(SimpleValue.FALSE));
    }

    private boolean toBoolean(DataItem value) {
        SimpleValue simpleValue = (SimpleValue) value;
        return (simpleValue.equals(SimpleValue.TRUE));
    }

    private boolean compareTags(Tag a, Tag b) {
        if (a == null) {
            return b == null;
        }
        if (b == null) {
            return false;
        }
        return a.getValue() == b.getValue();
    }
}
