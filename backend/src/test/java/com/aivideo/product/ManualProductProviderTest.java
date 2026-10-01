package com.aivideo.product;

import com.aivideo.product.dto.ProductFields;
import com.aivideo.product.dto.ProductRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ManualProductProviderTest {

    private final ManualProductProvider provider = new ManualProductProvider();

    private ProductFields fullFields() {
        return new ProductFields("Bo ngu 2 day", "Fashion", "Thun mem mat",
                List.of("mem", "mat"), "thun", "den", "M",
                "nu 18-30", List.of("mat", "re"), "199000 VND");
    }

    @Test
    void supportsWhenManualDataPresent() {
        assertThat(provider.supports(new ProductRequest(null, fullFields()))).isTrue();
        assertThat(provider.supports(new ProductRequest("http://shop/p/1", fullFields()))).isTrue();
    }

    @Test
    void doesNotSupportUrlOnlyOrEmpty() {
        assertThat(provider.supports(new ProductRequest("http://shop/p/1", null))).isFalse();
        assertThat(provider.supports(new ProductRequest("http://shop/p/1",
                new ProductFields(null, null, null, null, null, null, null, null, null, null))))
                .isFalse();
        assertThat(provider.supports(new ProductRequest(null, null))).isFalse();
    }

    @Test
    void resolveMapsAllFields() {
        ProductContext context = provider.resolve(
                new ProductRequest("http://shop/p/1", fullFields()));

        assertThat(context.name()).isEqualTo("Bo ngu 2 day");
        assertThat(context.category()).isEqualTo("Fashion");
        assertThat(context.features()).containsExactly("mem", "mat");
        assertThat(context.material()).isEqualTo("thun");
        assertThat(context.color()).isEqualTo("den");
        assertThat(context.size()).isEqualTo("M");
        assertThat(context.targetAudience()).isEqualTo("nu 18-30");
        assertThat(context.sellingPoints()).containsExactly("mat", "re");
        assertThat(context.price()).isEqualTo("199000 VND");
        assertThat(context.productUrl()).isEqualTo("http://shop/p/1");
    }
}
