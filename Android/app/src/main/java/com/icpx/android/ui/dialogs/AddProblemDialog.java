package com.icpx.android.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.icpx.android.R;
import com.icpx.android.model.Target;
import com.icpx.android.service.CodeforcesService;

/**
 * Dialog for adding a new problem
 */
public class AddProblemDialog extends Dialog {

    // Tab layouts
    private TabLayout inputMethodTabs;
    private View byLinkLayout;
    private View byContestIdLayout;
    private View byTopicLayout;

    // By Link fields
    private TextInputLayout problemLinkLayout;
    private TextInputLayout problemNameLayout;
    private TextInputLayout ratingLayout;
    private TextInputEditText problemLinkEditText;
    private TextInputEditText problemNameEditText;
    private TextInputEditText ratingEditText;
    private View fetchStatusLayout;
    private ProgressBar fetchProgressBar;
    private TextView fetchStatusText;

    // By Contest ID fields
    private TextInputLayout contestIdLayout;
    private TextInputEditText contestIdEditText;
    private Spinner problemIndexFromSpinner;
    private Spinner problemIndexToSpinner;
    private MaterialButton fetchByContestButton;
    private TextView contestFetchedInfo;

    // By Topic fields
    private TextInputLayout topicNameLayout;
    private TextInputLayout topicDescriptionLayout;
    private TextInputEditText topicNameEditText;
    private TextInputEditText topicDescriptionEditText;

    // Common buttons
    private MaterialButton cancelButton;
    private MaterialButton addButton;

    private OnProblemAddedListener listener;
    private CodeforcesService codeforcesService;
    private String currentMode = "link"; // link, contest, topic
    
    // Store fetched data from contest
    private String fetchedContestName = "";
    private int fetchedContestRating = 0;
    private String fetchedContestLink = "";

    public interface OnProblemAddedListener {
        void onProblemAdded(Target target);
    }

    public AddProblemDialog(@NonNull Context context, OnProblemAddedListener listener) {
        super(context);
        this.listener = listener;
        this.codeforcesService = new CodeforcesService();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_add_problem);

        // Set dialog width to 90% of screen width
        Window window = getWindow();
        if (window != null) {
            android.view.WindowManager.LayoutParams params = window.getAttributes();
            android.util.DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
            params.width = (int) (displayMetrics.widthPixels * 0.9);
            params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        // Tab layout
        inputMethodTabs = findViewById(R.id.inputMethodTabs);
        byLinkLayout = findViewById(R.id.byLinkLayout);
        byContestIdLayout = findViewById(R.id.byContestIdLayout);
        byTopicLayout = findViewById(R.id.byTopicLayout);

        // By Link fields
        problemLinkLayout = findViewById(R.id.problemLinkLayout);
        problemNameLayout = findViewById(R.id.problemNameLayout);
        ratingLayout = findViewById(R.id.ratingLayout);
        problemLinkEditText = findViewById(R.id.problemLinkEditText);
        problemNameEditText = findViewById(R.id.problemNameEditText);
        ratingEditText = findViewById(R.id.ratingEditText);
        fetchStatusLayout = findViewById(R.id.fetchStatusLayout);
        fetchProgressBar = findViewById(R.id.fetchProgressBar);
        fetchStatusText = findViewById(R.id.fetchStatusText);

        // By Contest ID fields
        contestIdLayout = findViewById(R.id.contestIdLayout);
        contestIdEditText = findViewById(R.id.contestIdEditText);
        problemIndexFromSpinner = findViewById(R.id.problemIndexFromSpinner);
        problemIndexToSpinner = findViewById(R.id.problemIndexToSpinner);
        fetchByContestButton = findViewById(R.id.fetchByContestButton);
        contestFetchedInfo = findViewById(R.id.contestFetchedInfo);

        // Setup problem index spinners
        String[] problemIndices = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "A1", "A2", "B1", "B2", "C1", "C2", "D1", "D2"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), 
                android.R.layout.simple_spinner_dropdown_item, problemIndices);
        problemIndexFromSpinner.setAdapter(adapter);
        problemIndexToSpinner.setAdapter(adapter);

        // By Topic fields
        topicNameLayout = findViewById(R.id.topicNameLayout);
        topicDescriptionLayout = findViewById(R.id.topicDescriptionLayout);
        topicNameEditText = findViewById(R.id.topicNameEditText);
        topicDescriptionEditText = findViewById(R.id.topicDescriptionEditText);

        // Common buttons
        cancelButton = findViewById(R.id.cancelButton);
        addButton = findViewById(R.id.addButton);
    }

    private void setupListeners() {
        // Tab selection
        inputMethodTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                switch (position) {
                    case 0:
                        currentMode = "link";
                        showLinkLayout();
                        break;
                    case 1:
                        currentMode = "contest";
                        showContestLayout();
                        break;
                    case 2:
                        currentMode = "topic";
                        showTopicLayout();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        cancelButton.setOnClickListener(v -> dismiss());
        addButton.setOnClickListener(v -> handleAddProblem());

        // Auto-fetch problem details when link is entered
        problemLinkEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String link = s.toString().trim();
                if (!link.isEmpty() && link.contains("codeforces.com")) {
                    fetchProblemDetails(link);
                }
            }
        });

        // Fetch by contest ID
        fetchByContestButton.setOnClickListener(v -> fetchByContestId());
    }

    private void showLinkLayout() {
        byLinkLayout.setVisibility(View.VISIBLE);
        byContestIdLayout.setVisibility(View.GONE);
        byTopicLayout.setVisibility(View.GONE);
    }

    private void showContestLayout() {
        byLinkLayout.setVisibility(View.GONE);
        byContestIdLayout.setVisibility(View.VISIBLE);
        byTopicLayout.setVisibility(View.GONE);
    }

    private void showTopicLayout() {
        byLinkLayout.setVisibility(View.GONE);
        byContestIdLayout.setVisibility(View.GONE);
        byTopicLayout.setVisibility(View.VISIBLE);
    }

    private void fetchProblemDetails(String link) {
        fetchStatusLayout.setVisibility(View.VISIBLE);
        fetchStatusText.setText(R.string.fetching_problem);
        addButton.setEnabled(false);

        new Thread(() -> {
            CodeforcesService.ProblemDetails details = 
                    codeforcesService.fetchProblemDetails(link);

            getContext().getMainExecutor().execute(() -> {
                if (details != null) {
                    problemNameEditText.setText(details.name);
                    ratingEditText.setText(String.valueOf(details.rating));
                    fetchStatusText.setText(R.string.problem_fetched);
                    addButton.setEnabled(true);
                } else {
                    fetchStatusText.setText(R.string.fetch_failed);
                    addButton.setEnabled(true);
                }
                
                fetchStatusLayout.postDelayed(() -> 
                        fetchStatusLayout.setVisibility(View.GONE), 2000);
            });
        }).start();
    }

    private void fetchByContestId() {
        String contestId = contestIdEditText.getText().toString().trim();
        String fromIndex = (String) problemIndexFromSpinner.getSelectedItem();
        String toIndex = (String) problemIndexToSpinner.getSelectedItem();

        if (contestId.isEmpty()) {
            Toast.makeText(getContext(), "Please enter Contest ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get problem indices array
        String[] allIndices = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "A1", "A2", "B1", "B2", "C1", "C2", "D1", "D2"};
        int fromPos = -1, toPos = -1;
        for (int i = 0; i < allIndices.length; i++) {
            if (allIndices[i].equals(fromIndex)) fromPos = i;
            if (allIndices[i].equals(toIndex)) toPos = i;
        }

        if (fromPos > toPos) {
            Toast.makeText(getContext(), "'From' must be <= 'To'", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create list of problems to fetch
        java.util.List<String> problemsToFetch = new java.util.ArrayList<>();
        for (int i = fromPos; i <= toPos; i++) {
            problemsToFetch.add(allIndices[i]);
        }
        
        contestFetchedInfo.setText("Fetching " + problemsToFetch.size() + " problem(s)...");
        contestFetchedInfo.setVisibility(View.VISIBLE);
        fetchByContestButton.setEnabled(false);
        addButton.setEnabled(false);

        new Thread(() -> {
            int successCount = 0;
            for (String index : problemsToFetch) {
                String link = "https://codeforces.com/contest/" + contestId + "/problem/" + index;
                CodeforcesService.ProblemDetails details = codeforcesService.fetchProblemDetails(link);

                if (details != null) {
                    // Create target and add it
                    Target target = new Target();
                    target.setName(details.name);
                    target.setType("problem");
                    target.setStatus("pending");
                    target.setRating(details.rating);
                    target.setProblemLink(link);
                    target.setCreatedAt(new java.util.Date());

                    if (getContext() == null) return;
                    
                    final int finalSuccess = ++successCount;
                    final int total = problemsToFetch.size();
                    getContext().getMainExecutor().execute(() -> {
                        if (listener != null) {
                            listener.onProblemAdded(target);
                        }
                        contestFetchedInfo.setText("Added " + finalSuccess + "/" + total + " problems");
                    });
                }
            }

            if (getContext() == null) return;
            
            final int finalSuccessCount = successCount;
            getContext().getMainExecutor().execute(() -> {
                if (finalSuccessCount > 0) {
                    Toast.makeText(getContext(), "Successfully added " + finalSuccessCount + " problem(s)", Toast.LENGTH_SHORT).show();
                    dismiss();
                } else {
                    contestFetchedInfo.setText("❌ Failed to fetch problems. Check Contest ID and internet connection.");
                    addButton.setEnabled(false);
                }
                fetchByContestButton.setEnabled(true);
            });
        }).start();
    }

    private void handleAddProblem() {
        Target target = null;

        switch (currentMode) {
            case "link":
                target = handleAddByLink();
                break;
            case "contest":
                // Contest problems are added directly by fetchByContestId
                Toast.makeText(getContext(), "Use 'Fetch Problems' button to add contest problems", Toast.LENGTH_SHORT).show();
                return;
            case "topic":
                target = handleAddByTopic();
                break;
        }

        if (target != null && listener != null) {
            listener.onProblemAdded(target);
            dismiss();
        }
    }

    private Target handleAddByLink() {
        String link = problemLinkEditText.getText().toString().trim();
        String name = problemNameEditText.getText().toString().trim();
        String ratingStr = ratingEditText.getText().toString().trim();

        if (link.isEmpty()) {
            problemLinkLayout.setError(getContext().getString(R.string.field_required));
            return null;
        }
        if (name.isEmpty()) {
            problemNameLayout.setError(getContext().getString(R.string.field_required));
            return null;
        }

        Target target = new Target("problem", name);
        target.setProblemLink(link);
        
        if (!ratingStr.isEmpty()) {
            try {
                target.setRating(Integer.parseInt(ratingStr));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        return target;
    }

    private Target handleAddByContest() {
        // Not used anymore - problems are added directly by fetchByContestId
        return null;
    }

    private Target handleAddByTopic() {
        String topicName = topicNameEditText.getText().toString().trim();
        String description = topicDescriptionEditText.getText().toString().trim();

        if (topicName.isEmpty()) {
            topicNameLayout.setError(getContext().getString(R.string.field_required));
            return null;
        }

        Target target = new Target("topic", topicName);
        if (!description.isEmpty()) {
            target.setDescription(description);
        }

        return target;
    }
}
