package edu.utsa.cs.privacy.violation;

import java.io.*;
import java.util.*;

/**
 * A driver to run Violation Detector
 *
 * @author sean
 */

public class DetectionMain {
    //Whether using APIs only from Susi, "susi" for susi only, "new" for the new API map
    public static final String SusiOnly = "new";
    //path to store api logs (output of flowdroid)
    private static final String API_FLOW_PATH = "C:/personal/ASE15_Privacy/newout_All/ApiFlow-All";
    //path to oracle file, need only for evaluation
    private static final String ORACLE_PATH = "C:/personal/ASE15_Privacy/data/violation_oracle.txt";
    //The path to the ontology file
    public static String TREE_PATH = "C:/personal/ASE15_Privacy/newout_All/study_utsa_resolved_words_Mitra-reconciled-w-round2-Final.owl";
    //The path to the API map file
    public static String API_MAP_PATH = "C:/personal/ASE15_Privacy/data/map_real_" + SusiOnly + ".txt";
    //path to the policies
    public static String POLICY_PATH = "C:/personal/ASE15_Privacy/data/policy_target_search";
    //path to output statistical files for each app
    public static String OUT_PATH = "C:/personal/ASE15_Privacy/data/ViolationOut";
    //path to output all violations
    public static String REPORT_PATH = "C:/personal/ASE15_Privacy/data/Violation.report";


    public static void main(String args[]) throws IOException, MultiOrNoRootException {
        ViolationDetector detector = new ViolationDetector(API_MAP_PATH,
                TREE_PATH);
        Hashtable<String, List<Violation>> finalResults = new Hashtable<String, List<Violation>>();
        File policyPath = new File(POLICY_PATH);
        PrintWriter pw = new PrintWriter(new FileWriter(REPORT_PATH));
        int count = 0;
        for (String name : policyPath.list()) {
            count = count + 1;
            String rootname = name.substring(0, name.length() - 4);
            if (!(new File(API_FLOW_PATH + "/" + rootname + ".apk.log").exists())) {
                continue;
            }
            List<Violation> violations = detector.detect(POLICY_PATH + "/"
                            + name, API_FLOW_PATH + "/" + rootname + ".apk.log",
                    rootname, true);
            if (violations.size() > 0) {
                for (Violation v : violations) {
                    pw.println(name + "\t" + v.toString());
                }
                finalResults.put(rootname, violations);
            }
        }
        pw.close();

        //The code below are for oracle checking and statistics to get the data for the paper
        List<Violation> oracles = loadOracle(ORACLE_PATH);
        printStats(finalResults);
        checkOracles(finalResults, oracles);
        getTops(finalResults, oracles);
    }

    private static void checkOracles(
            Hashtable<String, List<Violation>> finalResults,
            List<Violation> oracles) {
        int matchExp = 0;
        int expWrongType = 0;
        int expFp = 0;

        int matchImp = 0;
        int impWrongType = 0;
        int impFp = 0;

        int expMissed = 0;
        int impMissed = 0;

        Set<Violation> covered = new HashSet<Violation>();
        Set<String> keysE = new HashSet<String>();
        Set<String> keysI = new HashSet<String>();
        for (String key : finalResults.keySet()) {
            for (Violation v : finalResults.get(key)) {
                if (!oracles.contains(v)) {
                    System.err.println("Warning: " + key + "\t" + v.toString());
                } else {
                    Violation oracleViolation = oracles.get(oracles.indexOf(v));
                    if (oracleViolation.getType() == ViolationType.EXPLICIT) {
                        keysE.add(key);
                    }
                    if (oracleViolation.getType() == ViolationType.IMPLICIT) {
                        keysI.add(key);
                    }

                    covered.add(oracleViolation);
                    if (v.getType() == ViolationType.EXPLICIT) {
                        if (oracleViolation.getType() == ViolationType.EXPLICIT) {
                            matchExp++;
                        } else if (oracleViolation.getType() == ViolationType.IMPLICIT) {
                            expWrongType++;
                        } else {
                            expFp++;
                        }
                    } else if (v.getType() == ViolationType.IMPLICIT) {
                        if (oracleViolation.getType() == ViolationType.IMPLICIT) {
                            matchImp++;
                        } else if (oracleViolation.getType() == ViolationType.EXPLICIT) {
                            impWrongType++;
                        } else {
                            impFp++;
                        }
                    }
                }
            }
        }

        for (Violation v : oracles) {
            if (!covered.contains(v)) {
                if (v.getType() == ViolationType.EXPLICIT) {
                    expMissed++;
                } else if (v.getType() == ViolationType.IMPLICIT) {
                    impMissed++;
                }
            }
        }
        System.out.println("keysE:" + keysE.size());
        System.out.println("keysI:" + keysI.size());

        System.out.println("matchExp:" + matchExp);
        System.out.println("expFp:" + expFp);
        System.out.println("expWrongType:" + expWrongType);

        System.out.println("matchImp:" + matchImp);
        System.out.println("impFp:" + impFp);
        System.out.println("impWrongType:" + impWrongType);

        System.out.println("expMissed:" + expMissed);
        System.out.println("impMissed:" + impMissed);
    }

    private static void getTops(
            Hashtable<String, List<Violation>> finalResults, List<Violation> oracles) {
        Hashtable<String, Integer> apiMapE = new Hashtable<String, Integer>();
        Hashtable<String, Integer> apiMapI = new Hashtable<String, Integer>();

        Hashtable<String, Integer> termMapE = new Hashtable<String, Integer>();
        Hashtable<String, Integer> termMapI = new Hashtable<String, Integer>();
        Set<String> apis = new HashSet<String>();
        Set<String> termset = new HashSet<String>();
        int[] table = new int[10];

        for (String key : finalResults.keySet()) {
            Set<String> addedTerms = new HashSet<String>();
            int count = 0;
            for (Violation v : finalResults.get(key)) {
                if (oracles.contains(v)) {
                    Violation ov = oracles.get(oracles.indexOf(v));
                    count++;
                    String api = ov.getApi();
                    apis.add(api);
                    if (ov.getType() == ViolationType.IMPLICIT) {
                        add(api, apiMapI);
                    } else if (ov.getType() == ViolationType.EXPLICIT) {
                        add(api, apiMapE);
                    }
                    Set<String> terms = v.getMissingPhrases();
                    termset.addAll(terms);
                    for (String term : terms) {
                        if (!addedTerms.contains(term)) {
                            if (ov.getType() == ViolationType.IMPLICIT) {
                                add(term, termMapI);
                                addedTerms.add(term);
                            } else if (ov.getType() == ViolationType.EXPLICIT) {
                                add(term, termMapE);
                                addedTerms.add(term);
                            }
                        }
                    }
                }
            }
            if (count > 0) {
                int cat = count >= 10 ? 9 : count;
                table[cat - 1]++;
                System.out.println(key + ":" + count);
            }
        }
        for (String api : apis) {
            System.out.println(api + "\t" + (apiMapE.get(api) == null ? 0 : apiMapE.get(api)) + "\t" + (apiMapI.get(api) == null ? 0 : apiMapI.get(api)));
        }
        for (String term : termset) {
            System.out.println(term + "\t" + (termMapE.get(term) == null ? 0 : termMapE.get(term)) + "\t" + (termMapI.get(term) == null ? 0 : termMapI.get(term)));
        }
        for (int i = 0; i < table.length; i++) {
            System.out.println("numberOfApps-" + i + ":" + table[i]);
        }
    }

    private static void add(String key, Hashtable<String, Integer> map) {
        if (map.keySet().contains(key)) {
            map.put(key, map.get(key) + 1);
        } else {
            map.put(key, 1);
        }
    }

    private static List<Violation> loadOracle(String path) throws IOException {
        List<Violation> vios = new ArrayList<Violation>();
        BufferedReader in = new BufferedReader(new FileReader(path));
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            String[] items = line.split("\t");
            if (!items[5].startsWith("F")) {
                vios.add(new Violation(items[0], items[2], getType(items[1]), toList(items[3]), toList(items[4])));
            } else {
                vios.add(new Violation(items[0], items[2], ViolationType.FPOSITIVE, toList(items[3]), toList(items[4])));
            }
        }
        in.close();
        return vios;
    }

    private static Set<String> toList(String str) {
        Set<String> set = new HashSet<String>();
        if (str.indexOf('[') == -1) {
            return set;
        }
        String[] contents = str.substring(str.indexOf('[') + 1, str.indexOf(']')).split(",");
        for (String content : contents) {
            set.add(content.trim());
        }
        return set;
    }

    private static ViolationType getType(String str) {
        if (str.startsWith("E")) {
            return ViolationType.EXPLICIT;
        } else if (str.startsWith("I")) {
            return ViolationType.IMPLICIT;
        } else if (str.startsWith("N")) {
            return ViolationType.NONEXIST;
        } else {
            return ViolationType.FPOSITIVE;
        }
    }
    /*
    public static void infoCheck(List<Violation> violations, String logspath,
	    String rootname) throws IOException {
	String logPath = getPath(logspath, rootname);
	if(logPath.equals("")){
	    violations.clear();
	    return;
	}
	String result = getResult(logPath);
	for(int i = 0; i < violations.size(); i++){
	    Violation v = violations.get(i);
	    if(result.indexOf(v.getApi())==-1){
		violations.remove(v);
		i = i - 1;
	    }
	}

    }*/

    public static String getResult(String logPath) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(logPath));
        String ret = "";
        boolean flag = false;
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            if (line.trim().startsWith("[main] INFO soot.jimple.infoflow.Infoflow - Source lookup done")) {
                flag = true;
            }
            if (flag) {
                ret = ret + line;
            }
        }
        in.close();
        return ret;
    }
    /*
    private static String getPath(String logspath, String rootname) {
	File fpath = new File(logspath);
	for(String sub : fpath.list()){
	    if(sub.indexOf(rootname)!=-1){
		return logspath + "/" + sub;
	    }
	}
	return "";
    }*/

    /**
     * Print statistic results for detected violations
     *
     * @param results
     * @throws IOException
     */
    public static void printStats(Hashtable<String, List<Violation>> results)
            throws IOException {
        PrintWriter apps = new PrintWriter(new FileWriter(OUT_PATH
                + "/vioApps-exp.txt"));
        PrintWriter apis = new PrintWriter(new FileWriter(OUT_PATH
                + "/vioAPIs-exp.txt"));

        Set<String> apiset = new HashSet<String>();
        Set<String> appset = new HashSet<String>();
        int implicitAll = 0;
        int explicitAll = 0;
        int nonexistAll = 0;
        for (String key : results.keySet()) {
            List<Violation> violations = results.get(key);
            int explicit = 0;
            int implicit = 0;
            int nonexist = 0;
            for (Violation v : violations) {
                if (v.getType() == ViolationType.EXPLICIT) {
                    explicit = explicit + 1;
                    explicitAll = explicitAll + 1;
                    apiset.add(v.getApi());
                    appset.add(v.getAppName());
                } else if (v.getType() == ViolationType.IMPLICIT) {
                    implicitAll = implicitAll + 1;
                    implicit = implicit + 1;
                } else {
                    nonexistAll = nonexistAll + 1;
                    nonexist = nonexist + 1;
                }
                if (explicit > 0) {
                    // System.out.println(key + ":" + explicit + ":" +
                    // implicit);
                }
            }
            // System.out.println(key + ":" + explicit + ":" + implicit);
        }
        System.out.println("foundExplicit:" + explicitAll + "~" + "foundImplicit:" + implicitAll + "~" + "foundNonExist:" + nonexistAll);
        for (String api : apiset) {
            apis.println(api + " -> _SOURCE_");
        }
        for (String app : appset) {
            apps.println(app);
        }
        apis.close();
        apps.close();
    }
}
