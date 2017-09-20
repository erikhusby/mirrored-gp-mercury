package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.json.JSONException;
import org.json.JSONObject;

public class CustomizationValues {
    private String productPartNumber;
    private String quantity;
    private String price;
    private String customName;

    private String productName;

    public CustomizationValues(String productPartNumber, String quantity, String price, String customName) {
        this.productPartNumber = productPartNumber;
        this.quantity = quantity;
        this.price = price;
        this.customName = customName;
    }

    public String getProductPartNumber() {
        return productPartNumber;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getPrice() {
        return price;
    }

    public String getCustomName() {
        return customName;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public boolean isEmpty() {
        return StringUtils.isBlank(quantity) && StringUtils.isBlank(price) && StringUtils.isBlank(customName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof CustomizationValues)) {
            return false;
        }

        CustomizationValues that = (CustomizationValues) o;

        return new EqualsBuilder()
                .append(getProductPartNumber(), that.getProductPartNumber())
                .append(getQuantity(), that.getQuantity())
                .append(getPrice(), that.getPrice())
                .append(getCustomName(), that.getCustomName())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getProductPartNumber())
                .append(getQuantity())
                .append(getPrice())
                .append(getCustomName())
                .toHashCode();
    }

    @Override
    public String toString() {
        return "CustomizationValues{" +
               "productPartNumber='" + productPartNumber + '\'' +
               ", quantity='" + quantity + '\'' +
               ", price='" + price + '\'' +
               ", customName='" + customName + '\'' +
               ", productName='" + productName + '\'' +
               '}';
    }

    public JSONObject toJson() throws JSONException {
        JSONObject addOnCustomizationValues = new JSONObject();
        addOnCustomizationValues.put("price", getPrice());
        addOnCustomizationValues.put("quantity", getQuantity());
        addOnCustomizationValues.put("customName", getCustomName());

        JSONObject addOnCustomization = new JSONObject();
        addOnCustomization.put(getProductPartNumber(), addOnCustomizationValues);

        return addOnCustomization;
    }
}
