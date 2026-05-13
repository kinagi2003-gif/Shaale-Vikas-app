package com.example.shaalevikas;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int PICK_NEED_IMAGE = 1001;
    private static final String SCHOOL_UPI_ID = "schoolupi@bank";
    private static final String SCHOOL_PAYEE_NAME = "Shaale Vikas School Fund";

    private final List<Need> needs = new ArrayList<>();
    private final List<Donor> donors = new ArrayList<>();
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration needsListener;
    private ListenerRegistration donorsListener;
    private LinearLayout tabBar;
    private LinearLayout content;
    private int selectedTab = 0;
    private String currentUserName = "";
    private String currentUserRole = "";
    private Uri selectedFormImageUri;
    private ImageView currentImagePreview;
    private final NumberFormat rupees = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    private final int teal = Color.rgb(15, 118, 110);
    private final int tealDark = Color.rgb(11, 95, 89);
    private final int amber = Color.rgb(217, 119, 6);
    private final int ink = Color.rgb(31, 41, 55);
    private final int muted = Color.rgb(107, 114, 128);
    private final int page = Color.rgb(246, 247, 242);
    private final int line = Color.rgb(225, 228, 218);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        showLoginScreen();
    }

    private void showLoginScreen() {
        detachRealtimeListeners();
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(22));
        root.setBackgroundColor(page);
        scroll.addView(root);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20), dp(22), dp(20), dp(22));
        hero.setBackground(gradient(tealDark, teal, dp(18)));
        root.addView(hero, new LinearLayout.LayoutParams(-1, -2));

        hero.addView(text("SCHOOL-ALUMNI BRIDGE", 12, Color.rgb(213, 245, 239), Typeface.BOLD));
        TextView title = text("Shaale Vikas", 34, Color.WHITE, Typeface.BOLD);
        title.setPadding(0, dp(8), 0, 0);
        hero.addView(title);

        TextView subtitle = text("Track school needs, alumni pledges, and visible impact in one transparent place.", 15, Color.rgb(235, 253, 249), Typeface.NORMAL);
        subtitle.setPadding(0, dp(8), 0, dp(16));
        hero.addView(subtitle);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        hero.addView(stats);
        stats.addView(heroMetric("Live", "Firestore"), new LinearLayout.LayoutParams(0, -2, 1));
        stats.addView(heroMetric("Free", "Auth + Firestore"), new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(cardBg());
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.setMargins(0, dp(16), 0, 0);
        root.addView(card, cardLp);

        card.addView(text("Continue as", 20, ink, Typeface.BOLD));
        TextView note = text("Select your role and sign in with a Firebase account.", 13, muted, Typeface.NORMAL);
        note.setPadding(0, dp(4), 0, dp(12));
        card.addView(note);

        card.addView(roleCard("Alumni", "View priorities and pledge support", "Create as role: Alumni", teal, v -> showCredentialDialog("Alumni")));
        card.addView(roleCard("Headmaster / Admin", "Add needs, upload photos, update progress", "Create as role: Admin", amber, v -> showCredentialDialog("Admin")));

        LinearLayout accountActions = new LinearLayout(this);
        accountActions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(-1, dp(44));
        actionsLp.setMargins(0, dp(16), 0, 0);
        card.addView(accountActions, actionsLp);

        Button newUser = outlineButton("New User");
        newUser.setOnClickListener(v -> showNewUserDialog());
        accountActions.addView(newUser, new LinearLayout.LayoutParams(0, -1, 1));

        Button forgot = outlineButton("Forgot Password");
        forgot.setOnClickListener(v -> showForgotPasswordDialog());
        LinearLayout.LayoutParams forgotLp = new LinearLayout.LayoutParams(0, -1, 1);
        forgotLp.setMargins(dp(8), 0, 0, 0);
        accountActions.addView(forgot, forgotLp);

        TextView setup = text("Backend uses Firebase Auth + Firestore. Photos are local demo previews only.", 12, muted, Typeface.BOLD);
        setup.setGravity(Gravity.CENTER);
        setup.setPadding(0, dp(16), 0, 0);
        root.addView(setup);

        setContentView(scroll);
    }

    private void showCredentialDialog(String role) {
        LinearLayout form = dialogForm();
        EditText email = input("Email");
        EditText password = input("Password");
        password.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(email);
        form.addView(password);

        new AlertDialog.Builder(this)
                .setTitle(role.equals("Admin") ? "Headmaster / Admin Login" : "Alumni Login")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Login", (dialog, which) -> {
                    String enteredEmail = email.getText().toString().trim();
                    String enteredPassword = password.getText().toString().trim();
                    if (enteredEmail.isEmpty() || enteredPassword.isEmpty()) {
                        Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    signInWithFirebase(enteredEmail, enteredPassword, role);
                })
                .show();
    }

    private void signInWithFirebase(String email, String password, String selectedRole) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    loadUserProfile(user.getUid(), selectedRole);
                })
                .addOnFailureListener(error -> Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void loadUserProfile(String uid, String selectedRole) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    String name = valueOr(snapshot.getString("name"), selectedRole + " User");
                    String role = valueOr(snapshot.getString("role"), selectedRole);
                    if (!role.equals(selectedRole)) {
                        auth.signOut();
                        Toast.makeText(this, "This account is registered as " + role, Toast.LENGTH_LONG).show();
                        return;
                    }
                    loginAs(name, role);
                })
                .addOnFailureListener(error -> Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void showNewUserDialog() {
        LinearLayout form = dialogForm();
        EditText name = input("Full name");
        EditText email = input("Email");
        EditText role = input("Role: Alumni or Admin");
        EditText password = input("Password - minimum 6 characters");
        password.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(name);
        form.addView(email);
        form.addView(role);
        form.addView(password);

        new AlertDialog.Builder(this)
                .setTitle("New User Registration")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Create Account", (dialog, which) ->
                        createFirebaseAccount(
                                name.getText().toString().trim(),
                                email.getText().toString().trim(),
                                cleanRole(role.getText().toString()),
                                password.getText().toString().trim()))
                .show();
    }

    private void createFirebaseAccount(String name, String email, String role, String password) {
        if (name.isEmpty() || email.isEmpty() || password.length() < 6) {
            Toast.makeText(this, "Enter name, email, and a 6+ character password", Toast.LENGTH_LONG).show();
            return;
        }
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) return;
                    Map<String, Object> profile = new HashMap<>();
                    profile.put("name", name);
                    profile.put("email", email);
                    profile.put("role", role);
                    profile.put("createdAt", Timestamp.now());
                    db.collection("users").document(user.getUid()).set(profile)
                            .addOnSuccessListener(unused -> loginAs(name, role))
                            .addOnFailureListener(error -> Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(error -> Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void showForgotPasswordDialog() {
        LinearLayout form = dialogForm();
        EditText email = input("Registered email");
        form.addView(email);
        new AlertDialog.Builder(this)
                .setTitle("Forgot Password")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Send Reset Link", (dialog, which) -> {
                    String enteredEmail = email.getText().toString().trim();
                    if (enteredEmail.isEmpty()) {
                        Toast.makeText(this, "Enter registered email", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    auth.sendPasswordResetEmail(enteredEmail)
                            .addOnSuccessListener(unused -> Toast.makeText(this, "Password reset email sent", Toast.LENGTH_LONG).show())
                            .addOnFailureListener(error -> Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show());
                })
                .show();
    }

    private void loginAs(String name, String role) {
        currentUserName = name;
        currentUserRole = role;
        selectedTab = 0;
        buildShell();
        attachRealtimeListeners();
        render();
    }

    private void attachRealtimeListeners() {
        detachRealtimeListeners();
        needsListener = db.collection("needs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    needs.clear();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            needs.add(Need.fromDoc(doc));
                        }
                    }
                    if (content != null) render();
                });

        donorsListener = db.collection("pledges")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    donors.clear();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            donors.add(Donor.fromDoc(doc));
                        }
                    }
                    if (content != null) render();
                });
    }

    private void detachRealtimeListeners() {
        if (needsListener != null) {
            needsListener.remove();
            needsListener = null;
        }
        if (donorsListener != null) {
            donorsListener.remove();
            donorsListener = null;
        }
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(page);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(18), dp(20), dp(16));
        header.setBackgroundColor(teal);
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        header.addView(text("Shaale Vikas", 27, Color.WHITE, Typeface.BOLD));
        TextView subtitle = text("School-Alumni Bridge for transparent micro-needs", 14, Color.rgb(213, 245, 239), Typeface.NORMAL);
        subtitle.setPadding(0, dp(4), 0, 0);
        header.addView(subtitle);

        LinearLayout accountRow = new LinearLayout(this);
        accountRow.setOrientation(LinearLayout.HORIZONTAL);
        accountRow.setGravity(Gravity.CENTER_VERTICAL);
        accountRow.setPadding(0, dp(12), 0, 0);
        header.addView(accountRow);
        accountRow.addView(text(currentUserName + " - " + currentUserRole, 13, Color.WHITE, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));

        Button logout = new Button(this);
        logout.setText("Logout");
        logout.setAllCaps(false);
        logout.setTextSize(12);
        logout.setTextColor(tealDark);
        logout.setBackground(pill(Color.WHITE, Color.TRANSPARENT, dp(16)));
        logout.setOnClickListener(v -> {
            auth.signOut();
            detachRealtimeListeners();
            needs.clear();
            donors.clear();
            showLoginScreen();
        });
        accountRow.addView(logout, new LinearLayout.LayoutParams(dp(90), dp(38)));

        tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setPadding(dp(10), dp(10), dp(10), dp(8));
        tabBar.setBackgroundColor(Color.WHITE);
        root.addView(tabBar, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(24));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void render() {
        renderTabs();
        content.removeAllViews();
        if (selectedTab == 0) renderDashboard();
        if (isAdmin() && selectedTab == 1) renderAdmin();
        if ((isAdmin() && selectedTab == 2) || (!isAdmin() && selectedTab == 1)) renderDonors();
        if ((isAdmin() && selectedTab == 3) || (!isAdmin() && selectedTab == 2)) renderImpact();
    }

    private void renderTabs() {
        tabBar.removeAllViews();
        String[] tabs = isAdmin() ? new String[]{"Needs", "Admin", "Donors", "Impact"} : new String[]{"Needs", "Donors", "Impact"};
        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            Button button = new Button(this);
            button.setText(tabs[i]);
            button.setAllCaps(false);
            button.setTextSize(13);
            button.setTextColor(index == selectedTab ? Color.WHITE : tealDark);
            button.setBackground(pill(index == selectedTab ? teal : Color.TRANSPARENT, index == selectedTab ? teal : line, dp(18)));
            button.setOnClickListener(v -> {
                selectedTab = index;
                render();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1);
            lp.setMargins(dp(4), 0, dp(4), 0);
            tabBar.addView(button, lp);
        }
    }

    private void renderDashboard() {
        sectionTitle("Current School Priorities");
        summaryStrip();
        boolean hasOpenNeed = false;
        for (Need need : needs) {
            if (!need.completed) {
                hasOpenNeed = true;
                content.addView(needCard(need, !isAdmin()));
            }
        }
        if (!hasOpenNeed) emptyState(isAdmin() ? "No open needs yet. Use Admin to add the first priority." : "No open needs have been posted yet.");
    }

    private void summaryStrip() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(6), 0, dp(14));
        row.addView(metric("Open Needs", String.valueOf(openNeedCount())), new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(metric("Pledged", rupees.format(totalCollected())), new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(row);
    }

    private View metric(String label, String value) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        box.setBackground(cardBg());
        box.addView(text(value, 20, tealDark, Typeface.BOLD));
        box.addView(text(label, 12, muted, Typeface.BOLD));
        LinearLayout.LayoutParams margins = new LinearLayout.LayoutParams(-1, -2);
        margins.setMargins(dp(4), 0, dp(4), 0);
        box.setLayoutParams(margins);
        return box;
    }

    private View needCard(Need need, boolean showPledge) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(15), dp(16), dp(15));
        card.setBackground(cardBg());
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardLp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(text(need.title, 18, ink, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
        TextView chip = text(need.priority, 12, Color.WHITE, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(10), dp(4), dp(10), dp(4));
        chip.setBackground(pill(need.priority.equals("High") ? amber : teal, Color.TRANSPARENT, dp(14)));
        top.addView(chip);
        card.addView(top);

        TextView meta = text(need.category + " - Estimate " + rupees.format(need.estimate), 13, muted, Typeface.NORMAL);
        meta.setPadding(0, dp(5), 0, 0);
        card.addView(meta);
        TextView desc = text(need.description, 14, ink, Typeface.NORMAL);
        desc.setPadding(0, dp(9), 0, dp(10));
        card.addView(desc);

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(need.percent());
        card.addView(bar, new LinearLayout.LayoutParams(-1, dp(10)));
        TextView progress = text(need.percent() + "% collected - " + rupees.format(need.collected) + " of " + rupees.format(need.estimate), 13, tealDark, Typeface.BOLD);
        progress.setPadding(0, dp(8), 0, dp(8));
        card.addView(progress);

        if (need.imageUri != null) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setImageURI(need.imageUri);
            LinearLayout.LayoutParams imageLp = new LinearLayout.LayoutParams(-1, dp(150));
            imageLp.setMargins(0, dp(4), 0, dp(10));
            card.addView(image, imageLp);
        }

        if (showPledge) {
            Button pledge = actionButton("Pledge Support", teal);
            pledge.setOnClickListener(v -> showPledgeDialog(need));
            card.addView(pledge, new LinearLayout.LayoutParams(-1, dp(46)));

            Button pay = actionButton("Pay via UPI", amber);
            pay.setOnClickListener(v -> showUpiPaymentDialog(need));
            LinearLayout.LayoutParams payLp = new LinearLayout.LayoutParams(-1, dp(46));
            payLp.setMargins(0, dp(8), 0, 0);
            card.addView(pay, payLp);
        }
        return card;
    }

    private void renderAdmin() {
        sectionTitle("Headmaster Admin");
        TextView note = text("Edit the needs dashboard, upload proof photos, and mark completed work.", 14, muted, Typeface.NORMAL);
        note.setPadding(0, 0, 0, dp(12));
        content.addView(note);

        Button add = actionButton("+ Add New Need", amber);
        add.setOnClickListener(v -> showNeedEditor(null));
        content.addView(add, new LinearLayout.LayoutParams(-1, dp(48)));
        spacer(12);

        for (Need need : needs) {
            LinearLayout card = (LinearLayout) needCard(need, false);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(4), 0, 0);
            Button edit = smallButton("Edit", teal);
            edit.setOnClickListener(v -> showNeedEditor(need));
            row.addView(edit, new LinearLayout.LayoutParams(0, dp(42), 1));
            Button done = smallButton(need.completed ? "Reopen" : "Mark Done", need.completed ? amber : tealDark);
            done.setOnClickListener(v -> updateNeedCompleted(need));
            LinearLayout.LayoutParams doneLp = new LinearLayout.LayoutParams(0, dp(42), 1);
            doneLp.setMargins(dp(8), 0, 0, 0);
            row.addView(done, doneLp);
            card.addView(row);
            content.addView(card);
        }
    }

    private void renderDonors() {
        sectionTitle("Donor Hall of Fame");
        if (donors.isEmpty()) {
            emptyState("No pledges yet. Alumni pledges will appear here.");
            return;
        }
        for (Donor donor : donors) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16), dp(14), dp(16), dp(14));
            card.setBackground(cardBg());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, 0, 0, dp(10));
            content.addView(card, lp);
            card.addView(text(donor.name, 18, ink, Typeface.BOLD));
            TextView detail = text(donor.note + " - " + rupees.format(donor.amount), 14, muted, Typeface.NORMAL);
            detail.setPadding(0, dp(5), 0, 0);
            card.addView(detail);
            TextView payment = text(donor.paymentMethod + " - " + donor.paymentStatus, 12, tealDark, Typeface.BOLD);
            payment.setPadding(0, dp(4), 0, 0);
            card.addView(payment);
        }
    }

    private void renderImpact() {
        sectionTitle("Impact Photos");
        boolean hasImpact = false;
        for (Need need : needs) {
            if (need.completed) {
                hasImpact = true;
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(16), dp(14), dp(16), dp(14));
                card.setBackground(cardBg());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.setMargins(0, 0, 0, dp(12));
                content.addView(card, lp);
                card.addView(text(need.title, 18, ink, Typeface.BOLD));
                TextView status = text("Completed work - Alumni support verified", 13, tealDark, Typeface.BOLD);
                status.setPadding(0, dp(4), 0, dp(10));
                card.addView(status);
                if (need.imageUri != null) {
                    ImageView image = new ImageView(this);
                    image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    image.setImageURI(need.imageUri);
                    card.addView(image, new LinearLayout.LayoutParams(-1, dp(180)));
                } else {
                    emptyInside(card, "Photo preview is local in free backend mode.");
                }
            }
        }
        if (!hasImpact) emptyState("Completed work and before/after proof will appear here.");
    }

    private void showPledgeDialog(Need need) {
        LinearLayout form = dialogForm();
        EditText name = input("Your name / Alumni batch");
        EditText amount = input("Pledge amount or item value");
        amount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        EditText note = input("Message, item name, or contact note");
        form.addView(name);
        form.addView(amount);
        form.addView(note);

        new AlertDialog.Builder(this)
                .setTitle("Pledge for " + need.title)
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Confirm Pledge", (dialog, which) -> {
                    int pledgeAmount = parseInt(amount.getText().toString());
                    if (pledgeAmount <= 0 || name.getText().toString().trim().isEmpty()) {
                        Toast.makeText(this, "Enter name and pledge amount", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    savePledgeToFirebase(need, name.getText().toString().trim(), pledgeAmount, note.getText().toString().trim());
                })
                .show();
    }

    private void showUpiPaymentDialog(Need need) {
        LinearLayout form = dialogForm();
        EditText name = input("Your name / Alumni batch");
        EditText amount = input("Payment amount");
        amount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        EditText note = input("Message or phone number");
        form.addView(name);
        form.addView(amount);
        form.addView(note);

        TextView helper = text("UPI ID: " + SCHOOL_UPI_ID + "\nPayment opens in any installed UPI app. Admin should verify payment in bank records.", 12, muted, Typeface.BOLD);
        helper.setPadding(0, dp(10), 0, 0);
        form.addView(helper);

        new AlertDialog.Builder(this)
                .setTitle("Pay for " + need.title)
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Open UPI App", (dialog, which) -> {
                    int paymentAmount = parseInt(amount.getText().toString());
                    String donorName = name.getText().toString().trim();
                    if (paymentAmount <= 0 || donorName.isEmpty()) {
                        Toast.makeText(this, "Enter name and payment amount", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    openUpiApp(need, donorName, paymentAmount, note.getText().toString().trim());
                })
                .show();
    }

    private void openUpiApp(Need need, String donorName, int amount, String donorNote) {
        Uri uri = new Uri.Builder()
                .scheme("upi")
                .authority("pay")
                .appendQueryParameter("pa", SCHOOL_UPI_ID)
                .appendQueryParameter("pn", SCHOOL_PAYEE_NAME)
                .appendQueryParameter("tn", "Shaale Vikas - " + need.title)
                .appendQueryParameter("am", String.valueOf(amount))
                .appendQueryParameter("cu", "INR")
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        Intent chooser = Intent.createChooser(intent, "Pay with UPI");
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "No UPI app found on this device", Toast.LENGTH_LONG).show();
            return;
        }

        startActivity(chooser);
        String note = donorNote.isEmpty() ? "UPI payment initiated for " + need.title : donorNote + " - UPI payment initiated";
        savePledgeToFirebase(need, donorName, amount, note, "UPI", "Pending verification");
    }

    private void savePledgeToFirebase(Need need, String donorName, int pledgeAmount, String donorNote) {
        savePledgeToFirebase(need, donorName, pledgeAmount, donorNote, "Pledge", "Committed");
    }

    private void savePledgeToFirebase(Need need, String donorName, int pledgeAmount, String donorNote, String paymentMethod, String paymentStatus) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || need.id == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> pledge = new HashMap<>();
        pledge.put("needId", need.id);
        pledge.put("needTitle", need.title);
        pledge.put("name", donorName);
        pledge.put("note", donorNote.isEmpty() ? "Pledged for " + need.title : donorNote);
        pledge.put("amount", pledgeAmount);
        pledge.put("userId", user.getUid());
        pledge.put("paymentMethod", paymentMethod);
        pledge.put("paymentStatus", paymentStatus);
        pledge.put("createdAt", Timestamp.now());

        int nextCollected = Math.min(need.estimate, need.collected + pledgeAmount);
        db.collection("pledges").add(pledge)
                .addOnSuccessListener(doc -> db.collection("needs").document(need.id)
                        .update("collected", nextCollected)
                        .addOnSuccessListener(unused -> Toast.makeText(this, "Pledge saved", Toast.LENGTH_LONG).show())
                        .addOnFailureListener(error -> Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show()))
                .addOnFailureListener(error -> Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void showNeedEditor(Need editing) {
        selectedFormImageUri = editing == null ? null : editing.imageUri;
        LinearLayout form = dialogForm();
        currentImagePreview = new ImageView(this);
        currentImagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        currentImagePreview.setBackgroundColor(Color.rgb(230, 234, 226));
        if (selectedFormImageUri != null) currentImagePreview.setImageURI(selectedFormImageUri);
        form.addView(currentImagePreview, new LinearLayout.LayoutParams(-1, dp(120)));

        Button imageButton = actionButton("Choose Need Photo", tealDark);
        imageButton.setOnClickListener(v -> pickNeedImage());
        form.addView(imageButton, new LinearLayout.LayoutParams(-1, dp(44)));

        EditText title = input("Need title");
        EditText category = input("Category");
        EditText description = input("Description");
        EditText estimate = input("Cost estimate");
        EditText collected = input("Funds collected");
        EditText priority = input("Priority: High / Medium / Low");
        estimate.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        collected.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        if (editing != null) {
            title.setText(editing.title);
            category.setText(editing.category);
            description.setText(editing.description);
            estimate.setText(String.valueOf(editing.estimate));
            collected.setText(String.valueOf(editing.collected));
            priority.setText(editing.priority);
        }

        form.addView(title);
        form.addView(category);
        form.addView(description);
        form.addView(estimate);
        form.addView(collected);
        form.addView(priority);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);

        new AlertDialog.Builder(this)
                .setTitle(editing == null ? "Add Need" : "Edit Need")
                .setView(scroll)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String cleanTitle = title.getText().toString().trim();
                    int estimateValue = parseInt(estimate.getText().toString());
                    if (cleanTitle.isEmpty() || estimateValue <= 0) {
                        Toast.makeText(this, "Title and estimate are required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Need target = editing == null ? new Need("", "", "", 0, 0, "Medium") : editing;
                    target.title = cleanTitle;
                    target.category = category.getText().toString().trim();
                    target.description = description.getText().toString().trim();
                    target.estimate = estimateValue;
                    target.collected = Math.min(estimateValue, parseInt(collected.getText().toString()));
                    target.priority = cleanPriority(priority.getText().toString());
                    target.imageUri = selectedFormImageUri;
                    saveNeedToFirebase(target, editing == null);
                })
                .show();
    }

    private void saveNeedToFirebase(Need need, boolean isNew) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }
        String docId = isNew ? db.collection("needs").document().getId() : need.id;
        if (docId == null) return;
        writeNeedDoc(docId, need, user.getUid(), isNew);
    }

    private void writeNeedDoc(String docId, Need need, String userId, boolean isNew) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", need.title);
        data.put("category", need.category);
        data.put("description", need.description);
        data.put("estimate", need.estimate);
        data.put("collected", need.collected);
        data.put("priority", need.priority);
        data.put("completed", need.completed);
        data.put("imageMode", "local-preview-only");
        data.put("updatedAt", Timestamp.now());
        data.put("updatedBy", userId);
        if (isNew) {
            data.put("createdAt", Timestamp.now());
            data.put("createdBy", userId);
        }
        db.collection("needs").document(docId).set(data)
                .addOnSuccessListener(unused -> Toast.makeText(this, "Need saved", Toast.LENGTH_LONG).show())
                .addOnFailureListener(error -> Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void updateNeedCompleted(Need need) {
        if (need.id == null) return;
        db.collection("needs").document(need.id).update("completed", !need.completed, "updatedAt", Timestamp.now())
                .addOnFailureListener(error -> Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void pickNeedImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_NEED_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_NEED_IMAGE && resultCode == RESULT_OK && data != null) {
            selectedFormImageUri = data.getData();
            if (selectedFormImageUri != null) {
                try {
                    getContentResolver().takePersistableUriPermission(selectedFormImageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                    // Some gallery apps return temporary access only.
                }
                if (currentImagePreview != null) currentImagePreview.setImageURI(selectedFormImageUri);
            }
        }
    }

    private LinearLayout dialogForm() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(8), dp(4), dp(8), dp(4));
        return form;
    }

    private EditText input(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setSingleLine(false);
        editText.setTextColor(ink);
        editText.setHintTextColor(muted);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(8), 0, 0);
        editText.setLayoutParams(lp);
        return editText;
    }

    private Button actionButton(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(pill(color, Color.TRANSPARENT, dp(8)));
        return button;
    }

    private Button smallButton(String label, int color) {
        Button button = actionButton(label, color);
        button.setTextSize(13);
        return button;
    }

    private Button outlineButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(13);
        button.setTextColor(tealDark);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(pill(Color.WHITE, line, dp(8)));
        return button;
    }

    private View roleCard(String role, String description, String email, int accentColor, View.OnClickListener listener) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(pill(Color.rgb(250, 251, 247), line, dp(12)));
        card.setOnClickListener(listener);
        TextView mark = text(role.substring(0, 1), 24, Color.WHITE, Typeface.BOLD);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(pill(accentColor, Color.TRANSPARENT, dp(22)));
        card.addView(mark, new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams copyLp = new LinearLayout.LayoutParams(0, -2, 1);
        copyLp.setMargins(dp(12), 0, dp(8), 0);
        card.addView(copy, copyLp);
        copy.addView(text(role, 17, ink, Typeface.BOLD));
        TextView desc = text(description, 13, muted, Typeface.NORMAL);
        desc.setPadding(0, dp(2), 0, 0);
        copy.addView(desc);
        TextView hint = text(email, 12, accentColor, Typeface.BOLD);
        hint.setPadding(0, dp(5), 0, 0);
        copy.addView(hint);
        TextView arrow = text(">", 24, accentColor, Typeface.BOLD);
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow, new LinearLayout.LayoutParams(dp(24), -2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(10), 0, 0);
        card.setLayoutParams(lp);
        return card;
    }

    private View heroMetric(String value, String label) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setBackground(pill(Color.argb(40, 255, 255, 255), Color.argb(80, 255, 255, 255), dp(10)));
        box.addView(text(value, 20, Color.WHITE, Typeface.BOLD));
        box.addView(text(label, 12, Color.rgb(213, 245, 239), Typeface.BOLD));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(4), 0, dp(4), 0);
        box.setLayoutParams(lp);
        return box;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setLineSpacing(0, 1.08f);
        return view;
    }

    private void sectionTitle(String value) {
        TextView title = text(value, 22, ink, Typeface.BOLD);
        title.setPadding(0, 0, 0, dp(10));
        content.addView(title);
    }

    private void spacer(int heightDp) {
        content.addView(new View(this), new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    private void emptyState(String message) {
        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(18), dp(24), dp(18), dp(24));
        box.setGravity(Gravity.CENTER);
        box.setBackground(cardBg());
        TextView text = text(message, 15, muted, Typeface.NORMAL);
        text.setGravity(Gravity.CENTER);
        box.addView(text);
        content.addView(box, new LinearLayout.LayoutParams(-1, -2));
    }

    private void emptyInside(LinearLayout parent, String message) {
        TextView text = text(message, 14, muted, Typeface.NORMAL);
        text.setGravity(Gravity.CENTER);
        text.setPadding(dp(10), dp(24), dp(10), dp(24));
        text.setBackground(pill(Color.rgb(235, 238, 231), line, dp(8)));
        parent.addView(text, new LinearLayout.LayoutParams(-1, -2));
    }

    private GradientDrawable cardBg() {
        return pill(Color.WHITE, line, dp(8));
    }

    private GradientDrawable gradient(int start, int end, int radius) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private GradientDrawable pill(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        if (stroke != Color.TRANSPARENT) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String cleanPriority(String value) {
        String clean = value.trim();
        if (clean.equalsIgnoreCase("High")) return "High";
        if (clean.equalsIgnoreCase("Low")) return "Low";
        return "Medium";
    }

    private String cleanRole(String value) {
        String clean = value.trim();
        if (clean.equalsIgnoreCase("Headmaster") || clean.equalsIgnoreCase("Admin")) return "Admin";
        return "Alumni";
    }

    private int totalCollected() {
        int total = 0;
        for (Need need : needs) total += need.collected;
        return total;
    }

    private int openNeedCount() {
        int count = 0;
        for (Need need : needs) if (!need.completed) count++;
        return count;
    }

    private boolean isAdmin() {
        return currentUserRole.equals("Admin");
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }

    private static String valueOr(String text, String fallback) {
        return text == null || text.trim().isEmpty() ? fallback : text;
    }

    private static int intValue(Long value) {
        return value == null ? 0 : value.intValue();
    }

    private static class Need {
        String id;
        String title;
        String category;
        String description;
        int estimate;
        int collected;
        String priority;
        Uri imageUri;
        boolean completed;

        Need(String title, String category, String description, int estimate, int collected, String priority) {
            this.title = title;
            this.category = category;
            this.description = description;
            this.estimate = estimate;
            this.collected = collected;
            this.priority = priority;
        }

        int percent() {
            if (estimate <= 0) return 0;
            return Math.min(100, Math.round((collected * 100f) / estimate));
        }

        static Need fromDoc(DocumentSnapshot doc) {
            Need need = new Need(
                    value(doc.getString("title")),
                    value(doc.getString("category")),
                    value(doc.getString("description")),
                    intValue(doc.getLong("estimate")),
                    intValue(doc.getLong("collected")),
                    valueOr(doc.getString("priority"), "Medium"));
            need.id = doc.getId();
            Boolean completedValue = doc.getBoolean("completed");
            need.completed = completedValue != null && completedValue;
            return need;
        }
    }

    private static class Donor {
        String id;
        String name;
        String note;
        int amount;
        String paymentMethod;
        String paymentStatus;

        Donor(String name, String note, int amount) {
            this.name = name;
            this.note = note;
            this.amount = amount;
            this.paymentMethod = "Pledge";
            this.paymentStatus = "Committed";
        }

        static Donor fromDoc(DocumentSnapshot doc) {
            Donor donor = new Donor(
                    value(doc.getString("name")),
                    value(doc.getString("note")),
                    intValue(doc.getLong("amount")));
            donor.id = doc.getId();
            donor.paymentMethod = valueOr(doc.getString("paymentMethod"), "Pledge");
            donor.paymentStatus = valueOr(doc.getString("paymentStatus"), "Committed");
            return donor;
        }
    }
}
