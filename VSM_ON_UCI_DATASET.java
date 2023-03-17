import java.io.*;
import java.sql.SQLOutput;
import java.util.*;

public class VSM {
    private ArrayList<Ingredient> ingredientsTable;
    private ArrayList<Product> productsTable;
    private HashMap<String, Pair<Double, ArrayList<Doc>>> tokenList;    // For each token, store a normalization denominator and a list of drugs

    public VSM(HashMap<String, String> docs, HashMap<String, String> pList, ArrayList<String> ps) {
        productsTable = new ArrayList<>();
        tokenList = new HashMap<>();
        for (String p : ps) {
            String review = pList.getOrDefault(p, null);
            if (review != null) {
                review = "\"" + review + "\"";
            }
            productsTable.add(new Product(null, p, review, ""));
        }
        ingredientsTable = new ArrayList<>();

        docs.forEach((key, value) -> {
            // Assign id to each ingredient
            UUID ingID = UUID.randomUUID();
            String[] products = value.split(";");
            String productIds = "";
            for (String name : products) {
                Product t = searchProductsTable(name);
                if (t != null) {
                    // Building inverted index on ingredients
                    UUID id = null;
                    if (t.getId() == null) {
                        id = UUID.randomUUID();
                        t.setId(id);
                    } else {
                        id = t.getId();
                    }

                    if (productIds.indexOf(id + "") == -1) {
                        productIds += productIds.length() == 0 ? id : ";" + id;
                    }
                    t.setIngredients(t.getIngredients() + ingID + ";");

                    // Building VSM on drug name
                    String[] tokens = name.split("[ \"'.,#$%&()\\-\\*]+");
                    for (String token : tokens) {
                        token = token.trim();
                        if (token == null || token.length() == 0) continue;
                        Pair<Double, ArrayList<Doc>> tokenInfo = tokenList.getOrDefault(token, new Pair(0.0, new ArrayList<Doc>()));
                        Boolean match = false;
                        for (Doc d : tokenInfo.getRightObject()) {
                            if (d.getDocId() == id) {
                                d.setTw(d.getTw() + 1);
                                match = true;
                                break;
                            }
                        }
                        if (!match) {
                            tokenInfo.getRightObject().add(new Doc(id, 1.0));
                        }
                        tokenList.put(token, tokenInfo);
                    }
                } else {
                    // Debugging purpose
                    System.out.println(name);
                }
            }
            ingredientsTable.add(new Ingredient(ingID, key, productIds));
        });
        // Calculate tfidf and normalization
        tokenList.forEach((token, dList) -> {
            int df = dList.getRightObject().size();
            int N = tokenList.size();
            for (Doc d : dList.getRightObject()) {
                Double tfidf = (1+Math.log10(d.getTw())) * Math.log10(N * 1.0/df);
                d.setTw(tfidf);
                dList.setLeftObject(dList.getLeftObject() + Math.pow(tfidf, 2.0));
            }
            dList.setLeftObject(Math.sqrt(dList.getLeftObject().doubleValue()));
        });
    }

    private Product searchProductsTable(String name) {
        for (Product product : productsTable) {
            if (product.getName().equals(name)) {
                return product;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        HashMap<String, String> docs = new HashMap<>();;
        ArrayList<String> products = new ArrayList<>();
        HashMap<String, String> productsList = new HashMap<>();
        // Get drug review information
        try {
            File file = new File("./drugsComTrain_raw.csv");
            FileInputStream fis = new FileInputStream(file);
            Scanner scn = new Scanner(fis);
            scn.nextLine();
            while (scn.hasNextLine()) {
                String line = scn.nextLine();
                // Processing UCI ML Drug Review dataset
                String[] items = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");   // Ignore comma inside quotes
                // review is quoted in triple quotes, if no ending triple quotes, concatenate next line
                if (line.split("\"\"\"").length - 1 != 2) {
                    String nl = "";
                    while (scn.hasNextLine()) {
                        nl = scn.nextLine();
                        if (nl.indexOf("\"\"\"") == -1) {
                            // Skip white line
                            if (nl.length() != 0) {
                                line += nl;
                            }
                        } else {
                            line += nl;
                            break;
                        }
                    }
                    items = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                }
                if (items.length != 7) System.out.println(line);
                String name = items[1].toLowerCase();
                String condition = items[2].replaceAll("\"", "");
                String review = items[3];
                String rating = items[4];
                String date = items[5];
                String count = items[6];
                String formattedReview = condition + "~" + review.replaceAll("\"", "") + "~" + rating + "~" + date + "~" + count;
                if (productsList.containsKey(name)) {
                    productsList.put(name, productsList.get(name) + ";" + formattedReview);
                } else {
                    productsList.put(name, formattedReview);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Extracting drugs and ingredients
        try {
            File file = new File("./products.txt");
            FileInputStream fis = new FileInputStream(file);
            Scanner scn = new Scanner(fis);
            scn.nextLine();
            while (scn.hasNextLine()) {
                // Orange Book dataset is delimited by '~'
                String[] items = scn.nextLine().split("~");
                String product = items[2].toLowerCase();
                String[] productArray = product.split(";");

                // Populate a list of product names
                for (String p : productArray) {
                    if (!products.contains(p)) {
                        products.add(p);
                    }
                }

                // Populate inverted index for ingredients
                String[] ingredients = items[0].split(";");
                for (String ingredient : ingredients) {
                    ingredient = ingredient.toLowerCase();
                    if (docs.containsKey(ingredient)) {
                        String[] prevProducts = docs.get(ingredient).split(";");
                        for (String p : productArray) {
                            p = p.toLowerCase();
                            if (!Arrays.asList(prevProducts).contains(p)) {
                                docs.put(ingredient, docs.get(ingredient) + ";" + p);
                            }
                        }
                    } else {
                        docs.put(ingredient, product);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        VSM vsm = new VSM(docs, productsList, products);

        // Write ingredients table
        try {
            File outputFile = new File("ingredients.csv");
            FileOutputStream fos = new FileOutputStream(outputFile);
            PrintWriter pwt = new PrintWriter(fos);

            for (Ingredient ingredient : vsm.ingredientsTable) {
                pwt.write(ingredient.getId() + "^" + ingredient.getName() + "^" + ingredient.getProducts() + "\n");
            }
            pwt.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Write products table
        try {
            File outputFile = new File("products.csv");
            FileOutputStream fos = new FileOutputStream(outputFile);
            PrintWriter pwt = new PrintWriter(fos);

            for (Product product : vsm.productsTable) {
                pwt.write( product.getId() + "^" + product.getName() + "^" + product.getReview() + "^" + product.getIngredients() + "\n");
            }
            pwt.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Write VSM
        try {
            File outputFile = new File("vsm.csv");
            FileOutputStream fos = new FileOutputStream(outputFile);
            PrintWriter pwt = new PrintWriter(fos);

            vsm.tokenList.forEach((token, dList) -> {
                pwt.write(token + "^" + dList.getLeftObject() + "^");
                for (Doc d : dList.getRightObject()) {
                    pwt.write(d.getDocId() + "," + d.getTw() + ";");
                }
                pwt.write("\n");
            });
            pwt.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Test cases for Vector Space Model
        System.out.println("Running test cases for VSM Model");
        vsm.rankSearch("half");
        vsm.rankSearch("sulfates and dexamethasone");

        // Test cases for Inverted Index
        System.out.println("Running test cases for Inverted Index");
        vsm.findSubstitute(new String[] {"eptifibatide"});
        vsm.findSubstitute(new String[] {"acyclovir sodium", "acyclovir"});
    }

    // This one is just for test cases. Actual implementation is in the wep app run time
    private void rankSearch(String queryString) {
        System.out.println("Input query is: " + "\"" + queryString + "\"");
        String[] query = queryString.toLowerCase().split(" ");
        HashMap<UUID, Double> result = new HashMap<>();
        Double qMag = 0.0;
        for (String q: query) {
            Pair<Double, ArrayList<Doc>> token = tokenList.get(q);
            if (token == null) continue;
            Double tokenMagnitude = token.getLeftObject();
            ArrayList<Doc> docList = token.getRightObject();
            Double qtfidf = (1 + Math.log10(1)) * Math.log10(tokenList.size() * 1.0) / docList.size();
            qMag += Math.pow(qtfidf, 2);

            for (Doc doc : docList) {
                Double tokenWeight = doc.getTw();
                Double score = qtfidf * tokenWeight / tokenMagnitude;
                if(!result.containsKey(doc.getDocId())) {
                    result.put(doc.getDocId(), score);
                }
                else {
                    score += result.get(doc.getDocId());
                    result.put(doc.getDocId(), score);
                }
            }
        }
        final Double mag = qMag;
        ArrayList<Pair<UUID, Double>> sortedList = new ArrayList<>();
        result.replaceAll((key, value) -> value / mag);
        result.forEach((key, value) -> sortedList.add(new Pair(key, value)));
        Collections.sort(sortedList, new Comparator<Pair<UUID, Double>>() {
            @Override
            public int compare(Pair<UUID, Double> o1, Pair<UUID, Double> o2) {
                Double diff = o1.getRightObject() - o2.getRightObject();
                if (diff > 0) return -1;
                else if (diff < 0) return 1;
                else return 0;
            }
        });
        int i = 1;
        for (Pair p : sortedList) {
            String name = "";
            for (Product product : productsTable) {
                if (product.getId() == p.getLeftObject()) {
                    name = product.getName();
                    break;
                }
            }
            System.out.println("Ranked result: ");
            System.out.println("Rank " + i + ": " + name);
            i++;
            if (i == 11) break;
        }
    }

    private String mergeProducts(String pList1, String pList2) {
        String result = "";
        if (pList1.length() < pList2.length()) {
            String[] list = pList1.split(";");
            for (String s : list) {
                if (pList2.indexOf(s) != -1) {
                    result += s + ";";
                }
            }
        } else {
            String[] list = pList2.split(";");
            for (String s : list) {
                if (pList1.indexOf(s) != -1) {
                    result += s + ";";
                }
            }
        }
        return result;
    }

    private void findSubstitute(String[] ingredients) {
        System.out.println("Searching for drugs with ingredients: " + String.join(",", ingredients));
        Ingredient ingredient = searchIngredientsTable(ingredients[0]);
        String result = ingredient.getProducts();
        for (int i = 1; i < ingredients.length; i++) {
            Ingredient ingredient2 = searchIngredientsTable(ingredients[i]);
            result = mergeProducts(result, ingredient2.getProducts());
        }
        ArrayList<String> names = new ArrayList<>();
        for (String id: result.split(";")) {
            for (Product p : productsTable) {
                if ((p.getId() + "").equals(id)) {
                    names.add(p.getName());
                    break;
                }
            }
        }
        System.out.print("Products with ingredients [" + String.join(",", ingredients) + "] are: ");
        for (String name : names) {
            System.out.print(name + "; ");
        }
        System.out.println();
    }

    private Ingredient searchIngredientsTable(String ingredientName) {
        for (Ingredient ingredient : ingredientsTable) {
            if (ingredient.getName().equals(ingredientName)) {
                return ingredient;
            }
        }
        return null;
    }

    protected class Doc {
        private UUID docId;
        private Double tw;

        public Doc(UUID did, Double tw) {
            docId = did;
            this.tw = tw;
        }

        public UUID getDocId() {
            return docId;
        }

        public void setDocId(UUID docId) {
            this.docId = docId;
        }

        public Double getTw() {
            return tw;
        }

        public void setTw(Double tw) {
            this.tw = tw;
        }
    }


}
