package datart.data.provider.jdbc;

import datart.core.base.consts.ValueType;
import datart.core.base.consts.VariableTypeEnum;
import datart.core.data.provider.ScriptVariable;
import datart.data.provider.calcite.dialect.H2Dialect;
import datart.data.provider.script.ReplacementPair;
import datart.data.provider.script.VariablePlaceholder;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegexVariableResolverTest {

    @Test
    void replacesNestedAndDirectOccurrencesOfTheSameVariable() {
        String sql = "parent_path LIKE concat('%', $VAR$, '%') OR enterprise_code = $VAR$";

        String resolved = resolve(sql, queryVariable("VAR", ValueType.STRING, "aabbcc"));

        assertFalse(resolved.contains("$VAR$"), resolved);
        assertTrue(resolved.matches("(?is).*concat\\s*\\(\\s*'%'\\s*,\\s*'aabbcc'\\s*,\\s*'%'\\s*\\).*"), resolved);
        assertTrue(resolved.matches("(?is).*enterprise_code\\s*=\\s*'aabbcc'.*"), resolved);
    }

    @Test
    void preservesOrdinaryComparisonReplacement() {
        String resolved = resolve("age >= $AGE$", queryVariable("AGE", ValueType.NUMERIC, "18"));

        assertFalse(resolved.contains("$AGE$"), resolved);
        assertTrue(resolved.matches("(?is).*age\\s*>=\\s*18.*"), resolved);
    }

    private String resolve(String sql, ScriptVariable variable) {
        List<VariablePlaceholder> placeholders = RegexVariableResolver.resolve(
                H2Dialect.DEFAULT,
                sql,
                Collections.singletonMap(variable.getNameWithQuote(), variable));
        placeholders = placeholders.stream()
                .sorted(Comparator.comparingDouble(holder ->
                        holder instanceof SimpleVariablePlaceholder
                                ? 1000 + holder.getOriginalSqlFragment().length()
                                : -holder.getOriginalSqlFragment().length()))
                .collect(Collectors.toList());
        for (VariablePlaceholder placeholder : placeholders) {
            ReplacementPair pair = placeholder.replacementPair();
            sql = StringUtils.replaceIgnoreCase(sql, pair.getPattern(), pair.getReplacement());
        }
        return sql;
    }

    private ScriptVariable queryVariable(String name, ValueType valueType, String value) {
        return new ScriptVariable(
                name,
                VariableTypeEnum.QUERY,
                valueType,
                Collections.singleton(value),
                false);
    }
}
