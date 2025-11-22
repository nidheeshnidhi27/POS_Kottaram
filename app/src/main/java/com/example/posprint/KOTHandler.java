package com.example.posprint;

import static androidx.fragment.app.FragmentManager.TAG;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Locale;

public class KOTHandler {
    Context context;
    JSONObject response, details;
    int printerId;
    private static final String ESC_FONT_SIZE_LARGE = "\u001B" + "!" + (char) 51;  // Double width + height + bold
    private static final String ESC_FONT_SIZE_MEDIUM = "\u001B" + "!" + (char) 46;
    private static final String ESC_FONT_SIZE_SMALL = "\u001B" + "!" + (char) 23;
    private static final String ESC_FONT_SIZE_RESET = "\u001B" + "!" + (char) 0;
    public KOTHandler(Context context, JSONObject response, JSONObject details) {

        this.context = context;
        this.response = response;
        this.details = details;
    }
    /*public void handleKOT() {
        try {
            // Loop through each detail object (e.g., "1", "6")
            for (Iterator<String> keyIterator = details.keys(); keyIterator.hasNext(); ) {
                String key = keyIterator.next();
                JSONObject objectDetails = details.getJSONObject(key);
                // Get the printer ID and fetch printer details
                int printerId = objectDetails.getInt("printer");
                JSONObject printerDetails = getPrinterDetails(printerId, response.getJSONArray("printers"));
                if (printerDetails == null) {
                    Log.e(TAG, "Printer details not found for printer ID: " + printerId);
                    continue; // Skip this object if printer details are missing
                }
                // Extract printer IP and port
                String printerIP = printerDetails.optString("ip");
                int printerPort = Integer.parseInt(printerDetails.optString("port", "9100")); // Default to 9100 if port is not found
                // Format the text for the current detail object
                String formattedText = formatKOTText(response, objectDetails);
                // Create a new PrintConnection instance and execute
                PrintConnection printConnection = new PrintConnection(printerIP, printerPort, formattedText);
                printConnection.execute();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling KOT", e);
        }
    }*/


    public void handleKOT() {
        try {
            for (Iterator<String> keyIterator = details.keys(); keyIterator.hasNext(); ) {
                String key = keyIterator.next();
                JSONObject objectDetails = details.getJSONObject(key);

                // Get the printer ID
                printerId = objectDetails.getInt("printer");

                // Get printer details
                JSONObject printerDetails = getPrinterDetails(printerId, response.getJSONArray("printers"));
                if (printerDetails == null) {
                    Log.e(TAG, "Printer details not found for printer ID: " + printerId);
                    continue;
                }

                // Extract printer IP & Port
                String printerIP = printerDetails.optString("ip");
                int printerPort = Integer.parseInt(printerDetails.optString("port", "9100"));

                // Format the text
                String formattedText = formatKOTText(response, objectDetails);

                // 🔥 *** NEW LOGIC : Decide number of copies ***
                int copies;
                if (printerId == 9) {
                    copies = 1;      // Print once
                } else {
                    copies = 3;      // Print 3 times
                }

                Log.d("KOTHandler", "Printer ID " + printerId + " → copies = " + copies);

                // 🔥 Execute printing multiple times
                for (int i = 0; i < copies; i++) {
                    PrintConnection printConnection = new PrintConnection(printerIP, printerPort, formattedText);
                    printConnection.execute();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling KOT", e);
        }
    }



    private String formatKOTText(JSONObject response, JSONObject objectDetails) {
        StringBuilder formattedText = new StringBuilder();
        try {
            JSONObject orderDetails = objectDetails.getJSONObject("order_details");
            formattedText.append(ESC_FONT_SIZE_LARGE)
                    .append(centerText("KOT :Kitchen", true))
                    .append(ESC_FONT_SIZE_RESET).append("\n");
            formattedText.append("-".repeat(45)).append("\n");

            String type = orderDetails.optString("order_type", "");
            String orderNo = orderDetails.optString("order_no", "");
            String orderTime = orderDetails.optString("order_time", "");
            String waiter = orderDetails.optString("waiter_name", "");
            String customer = orderDetails.optString("customer_name", "");
            String address = orderDetails.optString("customer_address", "");
            String phone = orderDetails.optString("customer_phone", "");
            String tableno = orderDetails.optString("tableno", "");
            int tableSeats = orderDetails.optInt("table_seats", 0);

// Convert type to display string
            String displayType;
            switch (type) {
                case "dinein":
                    displayType = "Dine-In";
                    break;
                case "takeaway":
                    displayType = "Takeaway";
                    break;
                case "delivery":
                    displayType = "Delivery";
                    break;
                default:
                    displayType = type;
                    break;
            }

            formattedText.append("\nDate: ").append(orderTime)
                    .append("\nCustomer: ").append(customer);

            if (type.equals("delivery")) {
                if (!TextUtils.isEmpty(address)) {
                    formattedText.append("\nAddress: ").append(address);
                }
                if (!TextUtils.isEmpty(phone)) {
                    formattedText.append("\nPhone: ").append(phone);
                }
            }

            formattedText.append("\nServed by: ").append(waiter);

            if (type.equals("dinein")) {
                formattedText.append("\n").append(ESC_FONT_SIZE_LARGE).append(tableno).append(ESC_FONT_SIZE_RESET);
                formattedText.append("\nSeats: ").append(tableSeats);
            }

            formattedText.append("\n\n")
                    .append(ESC_FONT_SIZE_LARGE)
                    .append(centerText(displayType + " #" + orderNo, true))
                    .append(ESC_FONT_SIZE_RESET)
                    .append("\n\n");

            formattedText.append("-".repeat(45)).append("\n");

            JSONObject categories = objectDetails.getJSONObject("categories");
            for (Iterator<String> catIterator = categories.keys(); catIterator.hasNext(); ) {
                String category = catIterator.next();
                JSONObject items = categories.getJSONObject(category);

                if(printerId != 9) {
                    formattedText.append(ESC_FONT_SIZE_LARGE)
                            .append(centerText(category, true))
                            .append(ESC_FONT_SIZE_RESET).append("\n");

                    formattedText.append("-".repeat(45)).append("\n");
                }

                for (Iterator<String> itemIterator = items.keys(); itemIterator.hasNext(); ) {
                    String itemId = itemIterator.next();
                    JSONObject item = items.getJSONObject(itemId);

                    // For "BANQUET NIGHTS", print addons directly
                    if (category.equalsIgnoreCase("BANQUET NIGHTS")) {

                        // Addon is expected to be a nested JSONObject with categories
                        Object addonObj = item.opt("addon");
                        if (addonObj instanceof JSONObject) {
                            JSONObject addonGroups = (JSONObject) addonObj;
                            for (Iterator<String> groupIterator = addonGroups.keys(); groupIterator.hasNext(); ) {
                                String groupName = groupIterator.next();
                                JSONObject groupItems = addonGroups.optJSONObject(groupName);
                                if (groupItems != null && groupItems.length() > 0) {
                                    // Group title
                                    formattedText.append(ESC_FONT_SIZE_MEDIUM).append("\n").append(groupName).append(ESC_FONT_SIZE_RESET).append("\n");
                                    for (Iterator<String> subItemIterator = groupItems.keys(); subItemIterator.hasNext(); ) {
                                        String subItemKey = subItemIterator.next();
                                        JSONObject subAddon = groupItems.getJSONObject(subItemKey);
                                        String adName = subAddon.optString("ad_name");
                                        String adQty = subAddon.optString("ad_qty");
                                        formattedText.append(ESC_FONT_SIZE_LARGE).append("\n  ").append(adQty).append(" x ").append(adName).append(ESC_FONT_SIZE_RESET).append("\n");
                                    }
                                }
                            }
                        }

                        // Print 'other' if available
                        String other = item.optString("other");
                        if (other != null && !other.trim().isEmpty()) {
                            formattedText.append("\n").append("Note: ").append(other).append("\n");
                        }
                    }
                    else {
                        // For other categories: normal item + addons + other
                        formattedText.append(ESC_FONT_SIZE_LARGE)
                                .append(item.optString("quantity")).append(" x ").append(item.optString("item"))
                                .append(ESC_FONT_SIZE_RESET).append("\n");


                        // Print addons if present
                        Object addonObj = item.opt("addon");
                        if (addonObj instanceof JSONObject) {
                            JSONObject addons = (JSONObject) addonObj;
                            if (addons.length() > 0) {
                                for (Iterator<String> addonIterator = addons.keys(); addonIterator.hasNext(); ) {
                                    String addonKey = addonIterator.next();
                                    JSONObject addonItem = addons.getJSONObject(addonKey);
                                    String adName = addonItem.optString("ad_name");
                                    String adQty = addonItem.optString("ad_qty");
                                    formattedText.append(ESC_FONT_SIZE_LARGE).append("\n  ").append(adQty).append(" x ").append(adName).append(ESC_FONT_SIZE_RESET).append("\n");
                                }
                            }
                        }

                        String other = item.optString("other");
                        if (other != null && !other.trim().isEmpty()) {
                            formattedText.append("\n").append("Note: ").append(other).append("\n");
                        }
                    }
                    formattedText.append("\n");
                }
                    if (printerId != 9)
                        formattedText.append("\n").append("-".repeat(45)).append("\n");
            }
            if (printerId == 9)
                formattedText.append("\n").append("-".repeat(45)).append("\n");
            formattedText.append("Special Instruction: ")
                    .append(orderDetails.optString("instruction")).append("\n")
                    .append("-".repeat(45)).append("\n");

            formattedText.append(ESC_FONT_SIZE_MEDIUM).append("Requested for: ")
                    .append(orderDetails.optString("delivery_time")).append(ESC_FONT_SIZE_RESET).append("\n")
                    .append("-".repeat(45)).append("\n");

        } catch (Exception e) {
            Log.e(TAG, "Error formatting KOT text", e);
        }
        return formattedText.toString();
    }

    private String centerText(String text, boolean isDoubleWidth) {
        int fullLineWidth = 45;
        int visualTextLength = isDoubleWidth ? text.length() * 2 : text.length();
        int spaces = (fullLineWidth - visualTextLength) / 2;
        if (spaces < 0) spaces = 0;
        return " ".repeat(spaces) + text;
    }
    private JSONObject getPrinterDetails(int printerId, JSONArray printersArray) {
        for (int i = 0; i < printersArray.length(); i++) {
            try {
                JSONObject printer = printersArray.getJSONObject(i);
                if (printer.getInt("id") == printerId) {
                    return printer;
                }
            } catch (JSONException e) {
                Log.e("API", "Error fetching printer details", e);
            }
        }
        return null; // If no matching printer is found
    }
}