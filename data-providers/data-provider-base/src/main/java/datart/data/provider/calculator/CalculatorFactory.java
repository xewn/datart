package datart.data.provider.calculator;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;

public class CalculatorFactory {

    private final Map<String, Class<? extends AbstractCalculator>> calculatorTypes;

    CalculatorFactory(Iterable<AbstractCalculator> calculators) {
        Map<String, Class<? extends AbstractCalculator>> discovered = new TreeMap<>();
        for (AbstractCalculator calculator : calculators) {
            if (calculator == null || StringUtils.isBlank(calculator.type())) {
                throw new IllegalStateException("Calculator type cannot be blank");
            }
            if (discovered.putIfAbsent(calculator.type(), calculator.getClass()) != null) {
                throw new IllegalStateException("Duplicate calculator type: " + calculator.type());
            }
        }
        calculatorTypes = Collections.unmodifiableMap(new LinkedHashMap<>(discovered));
    }

    public static CalculatorFactory load() {
        return new CalculatorFactory(ServiceLoader.load(AbstractCalculator.class));
    }

    public AbstractCalculator create(String type) {
        Class<? extends AbstractCalculator> calculatorType = calculatorTypes.get(type);
        if (calculatorType == null) {
            throw new IllegalArgumentException("Unsupported calculator type: " + type);
        }
        try {
            return calculatorType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create calculator type: " + type, e);
        }
    }
}
