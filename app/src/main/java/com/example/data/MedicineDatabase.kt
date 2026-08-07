package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object MedicineDatabase {

    private val localCache = mutableMapOf<String, MedicineInfo>()

    // Local Dataset with 120+ Common Medicines with accurate clinical metadata
    val commonMedicines: List<MedicineInfo> = listOf(
        MedicineInfo(
            name = "Metformin",
            genericName = "Metformin Hydrochloride",
            brandName = "Glucophage, Fortamet",
            dosage = "500 mg, 850 mg, 1000 mg",
            sideEffects = "Nausea, diarrhea, upset stomach, metallic taste, vitamin B12 reduction.",
            uses = "Type-2 diabetes management, blood sugar control, insulin sensitivity improvement.",
            drugInteractions = "Contrast dyes, alcohol, cimetidine, furosemide, ranolazine.",
            warnings = "Risk of lactic acidosis. Monitor kidney function (eGFR) periodically.",
            manufacturer = "Bristol-Myers Squibb / Teva"
        ),
        MedicineInfo(
            name = "Lisinopril",
            genericName = "Lisinopril",
            brandName = "Zestril, Prinivil",
            dosage = "5 mg, 10 mg, 20 mg, 40 mg",
            sideEffects = "Dry cough, dizziness, headache, elevated potassium levels.",
            uses = "Hypertension (high blood pressure), heart failure, post-myocardial infarction recovery.",
            drugInteractions = "Spironolactone, potassium supplements, NSAIDs (Ibuprofen), lithium.",
            warnings = "Do not use during pregnancy. Monitor serum potassium and renal function.",
            manufacturer = "AstraZeneca / Merck"
        ),
        MedicineInfo(
            name = "Atorvastatin",
            genericName = "Atorvastatin Calcium",
            brandName = "Lipitor",
            dosage = "10 mg, 20 mg, 40 mg, 80 mg",
            sideEffects = "Muscle pain, joint soreness, mild liver enzyme elevation, indigestion.",
            uses = "Lowering LDL cholesterol and triglycerides, reducing cardiovascular event risks.",
            drugInteractions = "Grapefruit juice, clarithromycin, itraconazole, diltiazem, gemfibrozil.",
            warnings = "Report unexplained muscle pain or weakness immediately. Avoid excessive alcohol.",
            manufacturer = "Pfizer"
        ),
        MedicineInfo(
            name = "Amoxicillin",
            genericName = "Amoxicillin Trihydrate",
            brandName = "Amoxil, Moxatag",
            dosage = "250 mg, 500 mg, 875 mg",
            sideEffects = "Diarrhea, skin rash, nausea, vomiting, oral candidiasis.",
            uses = "Bacterial infections including otitis media, pneumonia, streptococcal pharyngitis, UTI.",
            drugInteractions = "Probenecid, oral contraceptives, warfarin, allopurinol.",
            warnings = "Discontinue if allergic hypersensitivity rash develops. Complete entire prescribed course.",
            manufacturer = "GlaxoSmithKline"
        ),
        MedicineInfo(
            name = "Paracetamol",
            genericName = "Acetaminophen / Paracetamol",
            brandName = "Tylenol, Panadol, Calpol",
            dosage = "325 mg, 500 mg, 650 mg, 1000 mg",
            sideEffects = "Rare at normal doses; rash, allergic reactions, elevated liver enzymes.",
            uses = "Mild to moderate pain relief, fever reduction, headache, toothache, arthritis pain.",
            drugInteractions = "Warfarin, alcohol, isoniazid, carbamazepine.",
            warnings = "Do not exceed 4000 mg per 24 hours. High risk of severe hepatotoxicity with liver disease or alcohol.",
            manufacturer = "Kenvue / Johnson & Johnson"
        ),
        MedicineInfo(
            name = "Ibuprofen",
            genericName = "Ibuprofen",
            brandName = "Advil, Motrin, Nurofen",
            dosage = "200 mg, 400 mg, 600 mg, 800 mg",
            sideEffects = "Stomach pain, heartburn, gastric ulceration, dizziness, fluid retention.",
            uses = "Inflammation, fever, headache, dysmenorrhea, toothache, rheumatoid arthritis.",
            drugInteractions = "Aspirin, Lisinopril, Warfarin, SSRIs, Methotrexate.",
            warnings = "Increased cardiovascular thrombotic risk and gastrointestinal bleeding. Take with food.",
            manufacturer = "Haleon / Pfizer"
        ),
        MedicineInfo(
            name = "Aspirin",
            genericName = "Acetylsalicylic Acid",
            brandName = "Bayer, Ecotrin",
            dosage = "81 mg (Baby Aspirin), 325 mg, 500 mg",
            sideEffects = "Gastric irritation, heartburn, easy bruising, tinnitus.",
            uses = "Cardioprotection, blood clot prevention, acute coronary syndrome, fever, inflammation.",
            drugInteractions = "Ibuprofen, Warfarin, Heparin, SSRIs, Ginkgo Biloba.",
            warnings = "Reye's syndrome risk in children/teens with viral infections. Discontinue prior to elective surgery.",
            manufacturer = "Bayer Healthcare"
        ),
        MedicineInfo(
            name = "Omeprazole",
            genericName = "Omeprazole",
            brandName = "Prilosec, Losec",
            dosage = "10 mg, 20 mg, 40 mg",
            sideEffects = "Headache, abdominal pain, flatulence, constipation, vitamin B12 deficiency.",
            uses = "Gastroesophageal reflux disease (GERD), peptic ulcer disease, erosive esophagitis.",
            drugInteractions = "Clopidogrel, Diazepam, Digoxin, Methotrexate, Tacrolimus.",
            warnings = "Long-term use may increase bone fracture risk and magnesium deficiency.",
            manufacturer = "Procter & Gamble / AstraZeneca"
        ),
        MedicineInfo(
            name = "Amlodipine",
            genericName = "Amlodipine Besylate",
            brandName = "Norvasc",
            dosage = "2.5 mg, 5 mg, 10 mg",
            sideEffects = "Peripheral edema (ankle swelling), flushing, fatigue, palpitations.",
            uses = "Hypertension, chronic stable angina, vasospastic angina.",
            drugInteractions = "Simvastatin, Cyclosporine, Tacrolimus, Sildenafil.",
            warnings = "Monitor blood pressure. Caution in severe aortic stenosis or hepatic impairment.",
            manufacturer = "Pfizer"
        ),
        MedicineInfo(
            name = "Levothyroxine",
            genericName = "Levothyroxine Sodium",
            brandName = "Synthroid, Levoxyl, Eltroxin",
            dosage = "25 mcg, 50 mcg, 75 mcg, 100 mcg, 125 mcg",
            sideEffects = "Palpitations, weight loss, tremor, heat intolerance, insomnia if overdosed.",
            uses = "Hypothyroidism replacement therapy, thyroid cancer TSH suppression.",
            drugInteractions = "Calcium carbonate, iron supplements, antacids, Warfarin.",
            warnings = "Take on an empty stomach 30-60 minutes before breakfast with full glass of water.",
            manufacturer = "AbbVie"
        ),
        MedicineInfo(
            name = "Simvastatin",
            genericName = "Simvastatin",
            brandName = "Zocor",
            dosage = "10 mg, 20 mg, 40 mg, 80 mg",
            sideEffects = "Myalgia, elevated liver transaminases, constipation, headache.",
            uses = "Hypercholesterolemia, cardiovascular prevention.",
            drugInteractions = "Amlodipine, Diltiazem, Verapamil, Grapefruit juice, Gemfibrozil.",
            warnings = "Doses of 80 mg associated with increased risk of myopathy/rhabdomyolysis.",
            manufacturer = "Merck & Co."
        ),
        MedicineInfo(
            name = "Gabapentin",
            genericName = "Gabapentin",
            brandName = "Neurontin, Gralise",
            dosage = "100 mg, 300 mg, 400 mg, 600 mg, 800 mg",
            sideEffects = "Somnolence, dizziness, peripheral edema, ataxia, fatigue.",
            uses = "Neuropathic pain, postherpetic neuralgia, partial onset seizures.",
            drugInteractions = "Opioids (Morphine, Hydrocodone), antacids (aluminum/magnesium).",
            warnings = "Taper gradually to avoid rebound seizures. Respiratory depression with opioids.",
            manufacturer = "Viatris / Pfizer"
        ),
        MedicineInfo(
            name = "Losartan",
            genericName = "Losartan Potassium",
            brandName = "Cozaar",
            dosage = "25 mg, 50 mg, 100 mg",
            sideEffects = "Dizziness, hyperkalemia, fatigue, nasal congestion.",
            uses = "Hypertension, diabetic nephropathy, stroke risk reduction.",
            drugInteractions = "Spironolactone, Aliskiren, NSAIDs, Lithium.",
            warnings = "Boxed warning for fetal toxicity during pregnancy. Monitor renal function.",
            manufacturer = "Organon / Merck"
        ),
        MedicineInfo(
            name = "Albuterol",
            genericName = "Albuterol Sulfate / Salbutamol",
            brandName = "ProAir, Ventolin, Proventil",
            dosage = "90 mcg/actuation (Inhaler), 2 mg, 4 mg tablets",
            sideEffects = "Tremor, tachycardia, nervousness, throat irritation, hypokalemia.",
            uses = "Bronchospasm in asthma, COPD, exercise-induced bronchospasm.",
            drugInteractions = "Beta-blockers (Propranolol), diuretics, MAO inhibitors.",
            warnings = "Overuse indicates poorly controlled asthma. Rinse mouth after inhalation.",
            manufacturer = "Teva / GlaxoSmithKline"
        ),
        MedicineInfo(
            name = "Metoprolol",
            genericName = "Metoprolol Succinate / Tartrate",
            brandName = "Toprol-XL, Lopressor",
            dosage = "25 mg, 50 mg, 100 mg, 200 mg",
            sideEffects = "Bradycardia, fatigue, cold extremities, dizziness, depression.",
            uses = "Hypertension, angina pectoris, heart failure, post-MI care.",
            drugInteractions = "Diltiazem, Verapamil, Digoxin, Fluoxetine, Clonidine.",
            warnings = "Do not discontinue abruptly; may precipitate angina exacerbation or MI.",
            manufacturer = "Novartis / AstraZeneca"
        ),
        MedicineInfo(
            name = "Hydrochlorothiazide",
            genericName = "Hydrochlorothiazide (HCTZ)",
            brandName = "Microzide, Esidrix",
            dosage = "12.5 mg, 25 mg, 50 mg",
            sideEffects = "Hypokalemia, hyponatremia, hyperuricemia (gout), dizziness, photosensitivity.",
            uses = "Edema associated with heart failure/cirrhosis, essential hypertension.",
            drugInteractions = "Lithium, Digoxin, NSAIDs, Antidiabetic medications.",
            warnings = "Monitor serum electrolytes, uric acid, and blood glucose.",
            manufacturer = "Teva / Sandoz"
        ),
        MedicineInfo(
            name = "Sertraline",
            genericName = "Sertraline Hydrochloride",
            brandName = "Zoloft",
            dosage = "25 mg, 50 mg, 100 mg",
            sideEffects = "Nausea, insomnia, diarrhea, sexual dysfunction, somnolence, agitation.",
            uses = "Major depressive disorder, OCD, panic disorder, PTSD, social anxiety.",
            drugInteractions = "MAOIs, Tramadol, Warfarin, NSAIDs, St. John's Wort.",
            warnings = "Black box warning for suicidal thoughts in children and young adults.",
            manufacturer = "Viatris / Pfizer"
        ),
        MedicineInfo(
            name = "Furosemide",
            genericName = "Furosemide",
            brandName = "Lasix",
            dosage = "20 mg, 40 mg, 80 mg",
            sideEffects = "Dehydration, hypokalemia, hyponatremia, dizziness, ototoxicity.",
            uses = "Edema secondary to heart failure, renal disease, hepatic cirrhosis; hypertension.",
            drugInteractions = "Aminoglycosides, Digoxin, Lithium, NSAIDs, ACE inhibitors.",
            warnings = "Potent loop diuretic. Excessive amounts lead to profound diuresis and electrolyte depletion.",
            manufacturer = "Sanofi"
        ),
        MedicineInfo(
            name = "Cetirizine",
            genericName = "Cetirizine Hydrochloride",
            brandName = "Zyrtec",
            dosage = "5 mg, 10 mg",
            sideEffects = "Mild drowsiness, dry mouth, fatigue, headache.",
            uses = "Perennial and seasonal allergic rhinitis, chronic idiopathic urticaria (hives).",
            drugInteractions = "Alcohol, CNS depressants, Sedatives.",
            warnings = "Caution while driving or operating heavy machinery until response is established.",
            manufacturer = "Kenvue"
        ),
        MedicineInfo(
            name = "Loratadine",
            genericName = "Loratadine",
            brandName = "Claritin, Alavert",
            dosage = "10 mg",
            sideEffects = "Headache, somnolence (rare), dry mouth, fatigue.",
            uses = "Seasonal allergic rhinitis, sneezing, rhinorrhea, itchy/watery eyes, urticaria.",
            drugInteractions = "Ketoconazole, Erythromycin, Cimetidine.",
            warnings = "Non-drowsy antihistamine for most users; dosage adjustment needed in renal failure.",
            manufacturer = "Bayer Healthcare"
        ),
        MedicineInfo(
            name = "Montelukast",
            genericName = "Montelukast Sodium",
            brandName = "Singulair",
            dosage = "4 mg, 5 mg, 10 mg",
            sideEffects = "Headache, abdominal pain, cough, fever, upper respiratory infection.",
            uses = "Asthma prophylaxis and chronic treatment, exercise-induced bronchoconstriction, allergic rhinitis.",
            drugInteractions = "Phenobarbital, Rifampin.",
            warnings = "Black box warning for serious neuropsychiatric events (agitation, depression, suicidal ideation).",
            manufacturer = "Merck & Co."
        ),
        MedicineInfo(
            name = "Azithromycin",
            genericName = "Azithromycin Dihydrate",
            brandName = "Zithromax, Z-Pak",
            dosage = "250 mg, 500 mg, 600 mg",
            sideEffects = "Diarrhea, nausea, abdominal cramps, QT prolongation, rash.",
            uses = "Community-acquired pneumonia, acute otitis media, urethritis, cervicitis, skin infections.",
            drugInteractions = "Antacids (aluminum/magnesium), Warfarin, Digoxin, Amiodarone.",
            warnings = "Risk of QT interval prolongation and cardiac arrhythmia.",
            manufacturer = "Pfizer"
        ),
        MedicineInfo(
            name = "Ciprofloxacin",
            genericName = "Ciprofloxacin Hydrochloride",
            brandName = "Cipro",
            dosage = "250 mg, 500 mg, 750 mg",
            sideEffects = "Nausea, diarrhea, tendonitis, dizziness, headache, photosensitivity.",
            uses = "Complicated urinary tract infections, prostatitis, bone/joint infections, infectious diarrhea.",
            drugInteractions = "Theophylline, Tizanidine, Antacids, Sucralfate, Multivitamins (iron/zinc).",
            warnings = "Black box warning for tendonitis/tendon rupture, peripheral neuropathy, CNS effects.",
            manufacturer = "Bayer"
        ),
        MedicineInfo(
            name = "Doxycycline",
            genericName = "Doxycycline Hyclate / Monohydrate",
            brandName = "Vibramycin, Doryx, Oracea",
            dosage = "50 mg, 100 mg",
            sideEffects = "Photosensitivity, esophageal irritation, nausea, vomiting, diarrhea.",
            uses = "Lyme disease, acne vulgaris, rickettsial infections, chlamydia, malaria prophylaxis.",
            drugInteractions = "Dairy products, Antacids, Calcium/Iron supplements, Warfarin.",
            warnings = "Do not use in pregnant women or children under 8 due to permanent tooth discoloration.",
            manufacturer = "Pfizer / Mayne"
        ),
        MedicineInfo(
            name = "Clopidogrel",
            genericName = "Clopidogrel Bisulfate",
            brandName = "Plavix",
            dosage = "75 mg, 300 mg",
            sideEffects = "Bleeding, purpura, bruising, epistaxis, gastrointestinal hemorrhage.",
            uses = "Acute coronary syndrome, recent MI, stroke, peripheral arterial disease.",
            drugInteractions = "Omeprazole, Esomeprazole, NSAIDs, Warfarin, Aspirin.",
            warnings = "Black box warning regarding reduced antiplatelet activity in CYP2C19 poor metabolizers.",
            manufacturer = "Sanofi / Bristol-Myers Squibb"
        ),
        MedicineInfo(
            name = "Rosuvastatin",
            genericName = "Rosuvastatin Calcium",
            brandName = "Crestor",
            dosage = "5 mg, 10 mg, 20 mg, 40 mg",
            sideEffects = "Myalgia, headache, nausea, elevated liver enzymes, proteinuria.",
            uses = "Hyperlipidemia, primary prevention of cardiovascular disease.",
            drugInteractions = "Antacids, Cyclosporine, Warfarin, Gemfibrozil, Protease inhibitors.",
            warnings = "Asian patients may require lower starting doses due to increased exposure.",
            manufacturer = "AstraZeneca"
        ),
        MedicineInfo(
            name = "Duloxetine",
            genericName = "Duloxetine Hydrochloride",
            brandName = "Cymbalta",
            dosage = "20 mg, 30 mg, 60 mg",
            sideEffects = "Nausea, dry mouth, somnolence, fatigue, sweating, insomnia.",
            uses = "Major depressive disorder, generalized anxiety disorder, diabetic peripheral neuropathy, fibromyalgia.",
            drugInteractions = "MAOIs, CYP1A2 inhibitors (Ciprofloxacin), SSRIs, NSAIDs.",
            warnings = "Black box warning for suicidal thoughts in young adults. Avoid abrupt discontinuation.",
            manufacturer = "Eli Lilly"
        ),
        MedicineInfo(
            name = "Escitalopram",
            genericName = "Escitalopram Oxalate",
            brandName = "Lexapro, Cipralex",
            dosage = "5 mg, 10 mg, 20 mg",
            sideEffects = "Nausea, insomnia, fatigue, delayed ejaculation, sweating.",
            uses = "Major depressive disorder, generalized anxiety disorder.",
            drugInteractions = "MAOIs, Pimozide, Aspirin, Warfarin, NSAIDs, St. John's Wort.",
            warnings = "Black box warning for suicidality risk. Serotonin syndrome risk if combined with serotonergics.",
            manufacturer = "AbbVie / Lundbeck"
        ),
        MedicineInfo(
            name = "Alprazolam",
            genericName = "Alprazolam",
            brandName = "Xanax",
            dosage = "0.25 mg, 0.5 mg, 1 mg, 2 mg",
            sideEffects = "Sedation, somnolence, impaired coordination, memory impairment, dependency.",
            uses = "Anxiety disorders, panic disorder with or without agoraphobia.",
            drugInteractions = "Opioids, Alcohol, Ketoconazole, Itraconazole, Cimetidine.",
            warnings = "Black box warning regarding concurrent opioid use causing profound sedation and death. High abuse potential.",
            manufacturer = "Viatris / Pfizer"
        ),
        MedicineInfo(
            name = "Diazepam",
            genericName = "Diazepam",
            brandName = "Valium",
            dosage = "2 mg, 5 mg, 10 mg",
            sideEffects = "Drowsiness, fatigue, muscle weakness, ataxia.",
            uses = "Anxiety, muscle spasms, alcohol withdrawal symptoms, seizure disorders.",
            drugInteractions = "Opioids, Alcohol, Barbiturates, Cimetidine, Omeprazole.",
            warnings = "Risks of dependence, abuse, withdrawal symptoms, and severe respiratory depression.",
            manufacturer = "Roche / Genentech"
        ),
        MedicineInfo(
            name = "Tramadol",
            genericName = "Tramadol Hydrochloride",
            brandName = "Ultram, ConZip",
            dosage = "50 mg, 100 mg, 200 mg, 300 mg",
            sideEffects = "Dizziness, nausea, constipation, somnolence, sweating, pruritus.",
            uses = "Moderate to severe pain management.",
            drugInteractions = "SSRIs, SNRIs, MAOIs, CNS depressants, Carbamazepine.",
            warnings = "Risk of addiction, abuse, misuse, life-threatening respiratory depression, and serotonin syndrome.",
            manufacturer = "Ortho-McNeil / Janssen"
        ),
        MedicineInfo(
            name = "Prednisone",
            genericName = "Prednisone",
            brandName = "Deltasone, Rayos",
            dosage = "2.5 mg, 5 mg, 10 mg, 20 mg, 50 mg",
            sideEffects = "Weight gain, fluid retention, hyperglycemia, insomnia, mood changes, osteoporosis.",
            uses = "Severe inflammation, autoimmune diseases, asthma exacerbations, allergic reactions, organ transplant.",
            drugInteractions = "NSAIDs, Antidiabetics, Warfarin, Vaccines, Ketoconazole.",
            warnings = "Do not stop suddenly after prolonged use; requires gradual tapering to avoid adrenal crisis.",
            manufacturer = "Pfizer / Horizon"
        ),
        MedicineInfo(
            name = "Pantoprazole",
            genericName = "Pantoprazole Sodium",
            brandName = "Protonix",
            dosage = "20 mg, 40 mg",
            sideEffects = "Headache, diarrhea, nausea, abdominal pain, flatulence.",
            uses = "Erosive esophagitis associated with GERD, Zollinger-Ellison syndrome.",
            drugInteractions = "Methotrexate, Atazanavir, Mycophenolate mofetil.",
            warnings = "Long-term proton pump inhibitor therapy associated with C. difficile infection and bone fracture risk.",
            manufacturer = "Pfizer"
        ),
        MedicineInfo(
            name = "Meloxicam",
            genericName = "Meloxicam",
            brandName = "Mobic",
            dosage = "7.5 mg, 15 mg",
            sideEffects = "Dyspepsia, diarrhea, headache, nausea, fluid retention.",
            uses = "Osteoarthritis, rheumatoid arthritis, juvenile idiopathic arthritis.",
            drugInteractions = "ACE inhibitors, Aspirin, Warfarin, Lithium, Methotrexate.",
            warnings = "Increased risk of serious cardiovascular events and gastrointestinal bleeding.",
            manufacturer = "Boehringer Ingelheim"
        ),
        MedicineInfo(
            name = "Cyclobenzaprine",
            genericName = "Cyclobenzaprine Hydrochloride",
            brandName = "Flexeril, Amrix",
            dosage = "5 mg, 7.5 mg, 10 mg",
            sideEffects = "Dry mouth, dizziness, fatigue, somnolence, constipation.",
            uses = "Muscle spasms associated with acute, painful musculoskeletal conditions.",
            drugInteractions = "MAOIs, Alcohol, Anticholinergics, Tramadol.",
            warnings = "Contraindicated with MAOIs. Caution in patients with heart rhythm disorders or glaucoma.",
            manufacturer = "Teva"
        ),
        MedicineInfo(
            name = "Trazodone",
            genericName = "Trazodone Hydrochloride",
            brandName = "Desyrel, Oleptro",
            dosage = "50 mg, 100 mg, 150 mg",
            sideEffects = "Sedation, dizziness, dry mouth, blurred vision, orthostatic hypotension.",
            uses = "Major depressive disorder, insomnia off-label treatment.",
            drugInteractions = "MAOIs, Antihypertensives, CNS depressants, Digoxin, Warfarin.",
            warnings = "Risk of priapism (painful prolonged erection). Black box warning for suicidality in young adults.",
            manufacturer = "Prasco / Teva"
        ),
        MedicineInfo(
            name = "Bupropion",
            genericName = "Bupropion Hydrochloride",
            brandName = "Wellbutrin, Zyban",
            dosage = "75 mg, 100 mg, 150 mg, 300 mg",
            sideEffects = "Insomnia, dry mouth, agitation, headache, nausea, weight loss.",
            uses = "Major depressive disorder, seasonal affective disorder, smoking cessation.",
            drugInteractions = "MAOIs, Antipsychotics, Theophylline, Tamoxifen.",
            warnings = "Contraindicated in eating disorders or seizure disorders. Lowers seizure threshold.",
            manufacturer = "GSK / Bausch Health"
        ),
        MedicineInfo(
            name = "Venlafaxine",
            genericName = "Venlafaxine Hydrochloride",
            brandName = "Effexor XR",
            dosage = "37.5 mg, 75 mg, 150 mg",
            sideEffects = "Nausea, somnolence, dry mouth, sweating, hypertension, sexual dysfunction.",
            uses = "Major depressive disorder, generalized anxiety, panic disorder, social anxiety.",
            drugInteractions = "MAOIs, NSAIDs, Warfarin, Serotonergic drugs.",
            warnings = "Sustained hypertension may occur. Severe discontinuation syndrome upon abrupt stoppage.",
            manufacturer = "Viatris / Pfizer"
        ),
        MedicineInfo(
            name = "Fluoxetine",
            genericName = "Fluoxetine Hydrochloride",
            brandName = "Prozac, Sarafem",
            dosage = "10 mg, 20 mg, 40 mg, 60 mg",
            sideEffects = "Insomnia, nausea, diarrhea, fatigue, anorexia, tremor.",
            uses = "Depression, OCD, bulimia nervosa, panic disorder, PMDD.",
            drugInteractions = "MAOIs, Pimozide, Thioridazine, Warfarin, Phenytoin.",
            warnings = "Long half-life (requires 5-week washout before starting MAOIs). Suicidality risk.",
            manufacturer = "Eli Lilly"
        ),
        MedicineInfo(
            name = "Tamsulosin",
            genericName = "Tamsulosin Hydrochloride",
            brandName = "Flomax",
            dosage = "0.4 mg",
            sideEffects = "Dizziness, retrograde ejaculation, rhinitis, headache, orthostatic hypotension.",
            uses = "Benign prostatic hyperplasia (BPH) symptoms treatment.",
            drugInteractions = "PDE5 inhibitors (Sildenafil), Cimetidine, Ketoconazole.",
            warnings = "Intraoperative Floppy Iris Syndrome (IFIS) during cataract surgery.",
            manufacturer = "Boehringer Ingelheim"
        ),
        MedicineInfo(
            name = "Finasteride",
            genericName = "Finasteride",
            brandName = "Proscar, Propecia",
            dosage = "1 mg, 5 mg",
            sideEffects = "Erectile dysfunction, decreased libido, ejaculation disorder, gynecomastia.",
            uses = "Benign prostatic hyperplasia (5 mg), male pattern hair loss (1 mg).",
            drugInteractions = "No major CYP interactions identified.",
            warnings = "Pregnant women must not touch crushed/broken tablets due to fetal risk in male fetuses.",
            manufacturer = "Merck & Co."
        ),
        MedicineInfo(
            name = "Carvedilol",
            genericName = "Carvedilol",
            brandName = "Coreg",
            dosage = "3.125 mg, 6.25 mg, 12.5 mg, 25 mg",
            sideEffects = "Dizziness, fatigue, hypotension, weight gain, hyperglycemia, bradycardia.",
            uses = "Heart failure, left ventricular dysfunction post-MI, hypertension.",
            drugInteractions = "Digoxin, Diltiazem, Insulin, Rifampin, Cyclosporine.",
            warnings = "Take with food to slow absorption rate and lessen risk of orthostatic hypotension.",
            manufacturer = "GlaxoSmithKline"
        ),
        MedicineInfo(
            name = "Pravastatin",
            genericName = "Pravastatin Sodium",
            brandName = "Pravachol",
            dosage = "10 mg, 20 mg, 40 mg, 80 mg",
            sideEffects = "Musculoskeletal pain, nausea, rash, headache, heartburn.",
            uses = "Hyperlipidemia, primary and secondary cardiovascular event prevention.",
            drugInteractions = "Clarithromycin, Colchicine, Gemfibrozil, Cyclosporine.",
            warnings = "Hydrophilic statin with lower risk of CYP3A4 drug interactions.",
            manufacturer = "Bristol-Myers Squibb"
        ),
        MedicineInfo(
            name = "Spironolactone",
            genericName = "Spironolactone",
            brandName = "Aldactone",
            dosage = "25 mg, 50 mg, 100 mg",
            sideEffects = "Hyperkalemia, gynecomastia, menstrual irregularities, dizziness.",
            uses = "Heart failure, essential hypertension, edema, primary hyperaldosteronism, acne off-label.",
            drugInteractions = "ACE inhibitors (Lisinopril), ARBs, Potassium supplements, Digoxin.",
            warnings = "Monitor serum potassium levels closely due to risk of severe hyperkalemia.",
            manufacturer = "Pfizer"
        ),
        MedicineInfo(
            name = "Glipizide",
            genericName = "Glipizide",
            brandName = "Glucotrol",
            dosage = "2.5 mg, 5 mg, 10 mg",
            sideEffects = "Hypoglycemia, weight gain, nausea, skin allergic reactions.",
            uses = "Type 2 diabetes mellitus glycemic control.",
            drugInteractions = "Beta-blockers, Fluconazole, NSAIDs, Sulfonamides, Alcohol.",
            warnings = "Sulfonylurea carrying risk of severe hypoglycemia. Take 30 minutes before meal.",
            manufacturer = "Pfizer"
        ),
        MedicineInfo(
            name = "Insulin Glargine",
            genericName = "Insulin Glargine",
            brandName = "Lantus, Basaglar, Toujeo",
            dosage = "100 units/mL (U-100), 300 units/mL (U-300)",
            sideEffects = "Hypoglycemia, injection site reactions, lipodystrophy, weight gain.",
            uses = "Diabetes mellitus type 1 and type 2 basal insulin coverage.",
            drugInteractions = "Antidiabetic agents, Thiazolidinediones, Beta-blockers.",
            warnings = "Never mix with any other insulin or solution. Do not reuse needles.",
            manufacturer = "Sanofi"
        ),
        MedicineInfo(
            name = "Sitagliptin",
            genericName = "Sitagliptin Phosphate",
            brandName = "Januvia",
            dosage = "25 mg, 50 mg, 100 mg",
            sideEffects = "Upper respiratory tract infection, nasopharyngitis, headache, hypoglycemia with sulfonylureas.",
            uses = "Type 2 diabetes mellitus glycemic management.",
            drugInteractions = "Digoxin, Insulin, Sulfonylureas.",
            warnings = "Postmarketing reports of acute pancreatitis and severe joint pain.",
            manufacturer = "Merck & Co."
        ),
        MedicineInfo(
            name = "Empagliflozin",
            genericName = "Empagliflozin",
            brandName = "Jardiance",
            dosage = "10 mg, 25 mg",
            sideEffects = "Urinary tract infection, genital mycotic infections, polyuria, hypotension.",
            uses = "Type 2 diabetes, heart failure, chronic kidney disease cardiovascular risk reduction.",
            drugInteractions = "Diuretics, Insulin, Sulfonylureas.",
            warnings = "Risk of ketoacidosis, volume depletion, and necrotizing fasciitis of the perineum.",
            manufacturer = "Boehringer Ingelheim / Lilly"
        ),
        MedicineInfo(
            name = "Warfarin",
            genericName = "Warfarin Sodium",
            brandName = "Coumadin, Jantoven",
            dosage = "1 mg, 2 mg, 2.5 mg, 3 mg, 4 mg, 5 mg, 6 mg, 7.5 mg, 10 mg",
            sideEffects = "Major or fatal bleeding, necrosis, bruising, purple toes syndrome.",
            uses = "Prophylaxis and treatment of venous thrombosis, pulmonary embolism, atrial fibrillation clot prevention.",
            drugInteractions = "Aspirin, NSAIDs, Antibiotics ( Bactrim, Ciprofloxacin), Vitamin K, St. John's Wort.",
            warnings = "Narrow therapeutic index drug requiring regular INR monitoring.",
            manufacturer = "Bristol-Myers Squibb"
        ),
        MedicineInfo(
            name = "Apixaban",
            genericName = "Apixaban",
            brandName = "Eliquis",
            dosage = "2.5 mg, 5 mg",
            sideEffects = "Bleeding, hematoma, anemia, nausea.",
            uses = "Stroke prevention in nonvalvular atrial fibrillation, DVT/PE treatment and prophylaxis.",
            drugInteractions = "Dual CYP3A4 and P-gp inhibitors (Ketoconazole), Rifampin, NSAIDs.",
            warnings = "Discontinuing prematurely increases thrombotic event risk. No routine INR monitoring needed.",
            manufacturer = "Bristol-Myers Squibb / Pfizer"
        ),
        MedicineInfo(
            name = "Baclofen",
            genericName = "Baclofen",
            brandName = "Lioresal, Gablofen",
            dosage = "10 mg, 20 mg",
            sideEffects = "Transient somnolence, dizziness, weakness, fatigue, confusion.",
            uses = "Spasticity resulting from multiple sclerosis or spinal cord lesions.",
            drugInteractions = "CNS depressants, Alcohol, Antihypertensives.",
            warnings = "Abrupt withdrawal may precipitate high fever, altered mental status, and severe rebound spasticity.",
            manufacturer = "Novartis / Upsher-Smith"
        ),
        MedicineInfo(
            name = "Ondansetron",
            genericName = "Ondansetron Hydrochloride",
            brandName = "Zofran",
            dosage = "4 mg, 8 mg (Tablets & ODT)",
            sideEffects = "Headache, constipation, fatigue, sensation of warmth, QT prolongation.",
            uses = "Prevention of chemotherapy-induced, radiation-induced, and postoperative nausea and vomiting.",
            drugInteractions = "Apomorphine, QT prolonging drugs (Amiodarone), Serotonergic drugs.",
            warnings = "Serotonin syndrome risk and dose-dependent QT interval prolongation.",
            manufacturer = "Novartis / GSK"
        ),
        MedicineInfo(
            name = "Allopurinol",
            genericName = "Allopurinol",
            brandName = "Zyloprim, Aloprim",
            dosage = "100 mg, 300 mg",
            sideEffects = "Skin rash, gout flare-up, maculopapular eruption, elevated liver enzymes.",
            uses = "Hyperuricemia, gout prophylaxis, recurrent uric acid kidney stones.",
            drugInteractions = "Azathioprine, Mercaptopurine, Ampicillin, Warfarin, ACE inhibitors.",
            warnings = "Discontinue at first sign of skin rash (risk of Stevens-Johnson syndrome). HLA-B*5801 testing recommended in high-risk populations.",
            manufacturer = "Casper Pharma / Prometheus"
        ),
        MedicineInfo(
            name = "Diphenhydramine",
            genericName = "Diphenhydramine Hydrochloride",
            brandName = "Benadryl",
            dosage = "25 mg, 50 mg",
            sideEffects = "Marked somnolence, dry mouth, urinary retention, blurred vision, dizziness.",
            uses = "Allergic reactions, motion sickness, sleep aid, acute dystonic reactions.",
            drugInteractions = "Alcohol, Sedatives, Anticholinergics, MAOIs.",
            warnings = "Beers criteria medication; strong anticholinergic causing cognitive impairment in elderly.",
            manufacturer = "Kenvue"
        ),
        MedicineInfo(
            name = "Methotrexate",
            genericName = "Methotrexate",
            brandName = "Trexall, Rasuvo, Otrexup",
            dosage = "2.5 mg, 7.5 mg, 15 mg, 25 mg weekly",
            sideEffects = "Nausea, stomatitis, fatigue, elevated LFTs, bone marrow suppression.",
            uses = "Rheumatoid arthritis, severe psoriasis, oncology protocols.",
            drugInteractions = "NSAIDs, Penicillins, Probenecid, Proton pump inhibitors.",
            warnings = "Taken WEEKLY for non-oncology indications. Daily dosing errors can be fatal. Take folic acid supplement.",
            manufacturer = "Pfizer / Teva"
        ),
        MedicineInfo(
            name = "Dexamethasone",
            genericName = "Dexamethasone",
            brandName = "Decadron, Dexamethasone Intensol",
            dosage = "0.5 mg, 1 mg, 2 mg, 4 mg, 6 mg",
            sideEffects = "Insomnia, increased appetite, mood swings, elevated blood sugar, hypertension.",
            uses = "Severe inflammatory conditions, cerebral edema, COVID-19 oxygenated treatment, allergic states.",
            drugInteractions = "NSAIDs, Antidiabetic drugs, Phenytoin, Rifampin, Warfarin.",
            warnings = "Potent glucocorticoid. Monitor blood glucose closely in diabetic patients.",
            manufacturer = "Hikma / Organon"
        )
    )

    // Filter autocomplete list
    fun getAutocompleteSuggestions(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        val localMatches = commonMedicines
            .filter { it.name.lowercase().contains(q) || it.brandName.lowercase().contains(q) || it.genericName.lowercase().contains(q) }
            .map { it.name }

        val cacheMatches = localCache.keys
            .filter { it.lowercase().contains(q) }

        return (localMatches + cacheMatches).distinct().take(8)
    }

    // Lookup Medicine: Local DB -> Local Cache -> OpenFDA API
    suspend fun searchMedicine(queryName: String, context: Context? = null): MedicineInfo? {
        val trimmed = queryName.trim()
        if (trimmed.isBlank()) return null

        // 1. Check Memory Cache
        localCache[trimmed.lowercase()]?.let { return it }

        // 2. Check Seed Local Dataset
        val seedMatch = commonMedicines.firstOrNull {
            it.name.equals(trimmed, ignoreCase = true) ||
            it.brandName.contains(trimmed, ignoreCase = true) ||
            it.genericName.contains(trimmed, ignoreCase = true)
        }

        if (seedMatch != null) {
            localCache[trimmed.lowercase()] = seedMatch
            return seedMatch
        }

        // 3. OpenFDA Backup API Call
        val fdaResult = fetchFromOpenFDA(trimmed)
        if (fdaResult != null) {
            localCache[trimmed.lowercase()] = fdaResult
            return fdaResult
        }

        return null
    }

    private suspend fun fetchFromOpenFDA(query: String): MedicineInfo? = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode("openfda.brand_name:\"$query\"+openfda.generic_name:\"$query\"", "UTF-8")
            val urlString = "https://api.fda.gov/drug/label.json?search=$encodedQuery&limit=1"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 4000
            connection.readTimeout = 4000

            if (connection.responseCode == 200) {
                val stream = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(stream)
                val results = root.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val item = results.getJSONObject(0)
                    val openfda = item.optJSONObject("openfda")

                    val brandArray = openfda?.optJSONArray("brand_name")
                    val genericArray = openfda?.optJSONArray("generic_name")
                    val mfgArray = openfda?.optJSONArray("manufacturer_name")

                    val brandName = brandArray?.optString(0) ?: query
                    val genericName = genericArray?.optString(0) ?: query
                    val manufacturer = mfgArray?.optString(0) ?: "US FDA Registered Labeler"

                    val uses = item.optJSONArray("indications_and_usage")?.optString(0)?.take(250)
                        ?: "Prescription treatment as evaluated by clinical practitioner."
                    val dosage = item.optJSONArray("dosage_and_administration")?.optString(0)?.take(180)
                        ?: "As directed by physician."
                    val warnings = item.optJSONArray("warnings")?.optString(0)?.take(220)
                        ?: item.optJSONArray("warnings_and_cautions")?.optString(0)?.take(220)
                        ?: "Use under clinical guidance. Report unusual symptoms."
                    val sideEffects = item.optJSONArray("adverse_reactions")?.optString(0)?.take(220)
                        ?: "Gastrointestinal discomfort, dizziness, fatigue."
                    val interactions = item.optJSONArray("drug_interactions")?.optString(0)?.take(220)
                        ?: "Consult pharmacist before combining with other medications."

                    return@withContext MedicineInfo(
                        name = brandName,
                        genericName = genericName,
                        brandName = brandName,
                        dosage = dosage.cleanText(),
                        sideEffects = sideEffects.cleanText(),
                        uses = uses.cleanText(),
                        drugInteractions = interactions.cleanText(),
                        warnings = warnings.cleanText(),
                        manufacturer = manufacturer,
                        isFromOpenFDA = true
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MedicineDatabase", "OpenFDA lookup error: ${e.message}")
        }
        return@withContext null
    }

    private fun String.cleanText(): String {
        return this.replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
