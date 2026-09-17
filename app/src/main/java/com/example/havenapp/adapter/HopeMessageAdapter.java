package com.example.havenapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.havenapp.R;
import com.example.havenapp.model.HopeMessage;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class HopeMessageAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    // =====================================================
    // VIEW TYPES
    // =====================================================

    private static final int VIEW_TYPE_WELCOME =
            HopeMessage.TYPE_WELCOME;

    private static final int VIEW_TYPE_HOPE =
            HopeMessage.TYPE_HOPE;

    private static final int VIEW_TYPE_USER =
            HopeMessage.TYPE_USER;


    // =====================================================
    // SUGGESTION CLICK LISTENER
    // =====================================================

    public interface OnSuggestionClickListener {

        void onSuggestionClick(
                String question
        );
    }


    // =====================================================
    // HOPE ACTION CLICK LISTENER
    // =====================================================

    /*
     * This sends Hope action-button clicks back to
     * ChatbotActivity.
     *
     * The adapter itself does NOT perform navigation.
     *
     * Examples:
     *
     * legal_article
     * educational_article
     * emergency_hotlines
     * file_report
     * emergency_help
     */

    public interface OnHopeActionClickListener {

        void onHopeActionClick(
                HopeMessage message
        );
    }


    // =====================================================
    // RELATED QUESTION CLICK LISTENER
    // =====================================================

    /*
     * Reserved for clickable related questions later.
     *
     * For now, related questions are displayed as text.
     * The existing welcome suggestions remain clickable.
     */

    public interface OnRelatedQuestionClickListener {

        void onRelatedQuestionClick(
                String question
        );
    }


    // =====================================================
    // DATA
    // =====================================================

    private final List<HopeMessage> messages;

    private final OnSuggestionClickListener
            suggestionClickListener;

    private OnHopeActionClickListener
            hopeActionClickListener;

    private OnRelatedQuestionClickListener
            relatedQuestionClickListener;


    // =====================================================
    // EXISTING CONSTRUCTOR
    // =====================================================

    /*
     * IMPORTANT:
     *
     * Keep the existing constructor so the current
     * ChatbotActivity does not break.
     */

    public HopeMessageAdapter(
            List<HopeMessage> messages,
            OnSuggestionClickListener suggestionClickListener
    ) {

        this.messages = messages;

        this.suggestionClickListener =
                suggestionClickListener;
    }


    // =====================================================
    // ACTION LISTENER SETTER
    // =====================================================

    public void setOnHopeActionClickListener(
            OnHopeActionClickListener listener
    ) {

        this.hopeActionClickListener =
                listener;
    }


    // =====================================================
    // RELATED QUESTION LISTENER SETTER
    // =====================================================

    public void setOnRelatedQuestionClickListener(
            OnRelatedQuestionClickListener listener
    ) {

        this.relatedQuestionClickListener =
                listener;
    }


    // =====================================================
    // VIEW TYPE
    // =====================================================

    @Override
    public int getItemViewType(
            int position
    ) {

        HopeMessage message =
                messages.get(position);

        return message.getType();
    }


    // =====================================================
    // CREATE VIEW HOLDER
    // =====================================================

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        LayoutInflater inflater =
                LayoutInflater.from(
                        parent.getContext()
                );


        // -------------------------------------------------
        // WELCOME
        // -------------------------------------------------

        if (viewType == VIEW_TYPE_WELCOME) {

            View view =
                    inflater.inflate(
                            R.layout.item_hope_welcome,
                            parent,
                            false
                    );

            return new WelcomeViewHolder(
                    view
            );
        }


        // -------------------------------------------------
        // USER MESSAGE
        // -------------------------------------------------

        if (viewType == VIEW_TYPE_USER) {

            View view =
                    inflater.inflate(
                            R.layout.item_user_message,
                            parent,
                            false
                    );

            return new UserMessageViewHolder(
                    view
            );
        }


        // -------------------------------------------------
        // HOPE MESSAGE
        // -------------------------------------------------

        View view =
                inflater.inflate(
                        R.layout.item_hope_message,
                        parent,
                        false
                );

        return new HopeMessageViewHolder(
                view
        );
    }


    // =====================================================
    // BIND VIEW HOLDER
    // =====================================================

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position
    ) {

        HopeMessage message =
                messages.get(position);


        // -------------------------------------------------
        // WELCOME
        // -------------------------------------------------

        if (holder instanceof WelcomeViewHolder) {

            WelcomeViewHolder welcomeHolder =
                    (WelcomeViewHolder) holder;

            welcomeHolder.bindSuggestions(
                    suggestionClickListener
            );

            return;
        }


        // -------------------------------------------------
        // USER MESSAGE
        // -------------------------------------------------

        if (holder instanceof UserMessageViewHolder) {

            UserMessageViewHolder userHolder =
                    (UserMessageViewHolder) holder;

            userHolder.txtMessage.setText(
                    safeText(
                            message.getMessage()
                    )
            );

            return;
        }


        // -------------------------------------------------
        // HOPE MESSAGE
        // -------------------------------------------------

        if (holder instanceof HopeMessageViewHolder) {

            HopeMessageViewHolder hopeHolder =
                    (HopeMessageViewHolder) holder;

            bindHopeMessage(
                    hopeHolder,
                    message
            );
        }
    }


    // =====================================================
    // BIND HOPE MESSAGE
    // =====================================================

    private void bindHopeMessage(
            HopeMessageViewHolder holder,
            HopeMessage message
    ) {

        // =================================================
        // MAIN MESSAGE
        // =================================================

        holder.txtMessage.setText(
                safeText(
                        message.getMessage()
                )
        );


        // =================================================
        // ACTION BUTTON
        // =================================================

        if (message.hasAction()) {

            holder.btnAction.setVisibility(
                    View.VISIBLE
            );

            holder.btnAction.setText(
                    message.getActionLabel()
            );

            holder.btnAction.setOnClickListener(
                    v -> {

                        if (hopeActionClickListener != null) {

                            hopeActionClickListener
                                    .onHopeActionClick(
                                            message
                                    );
                        }
                    }
            );

        } else {

            /*
             * RecyclerView reuses rows.
             *
             * Always reset the visibility and click listener
             * when this message has no action.
             */

            holder.btnAction.setVisibility(
                    View.GONE
            );

            holder.btnAction.setOnClickListener(
                    null
            );
        }


        // =================================================
        // SOURCE
        // =================================================

        if (message.hasSource()) {

            holder.layoutSource.setVisibility(
                    View.VISIBLE
            );

            holder.txtSource.setText(
                    message.getSourceName()
            );

        } else {

            holder.layoutSource.setVisibility(
                    View.GONE
            );

            holder.txtSource.setText(
                    ""
            );
        }


        // =================================================
        // RELATED QUESTIONS
        // =================================================

        if (message.hasRelatedQuestions()) {

            holder.layoutRelated.setVisibility(
                    View.VISIBLE
            );

            holder.txtRelated.setText(
                    message.getRelatedQuestions()
            );


            /*
             * For now the entire related-question area can
             * optionally be clicked.
             *
             * Later we can replace this with individual
             * clickable suggestion chips if desired.
             */

            if (relatedQuestionClickListener != null) {

                holder.txtRelated.setOnClickListener(
                        v -> {

                            String related =
                                    message.getRelatedQuestions();

                            if (related != null
                                    && !related.trim().isEmpty()) {

                                relatedQuestionClickListener
                                        .onRelatedQuestionClick(
                                                related.trim()
                                        );
                            }
                        }
                );

            } else {

                holder.txtRelated.setOnClickListener(
                        null
                );
            }

        } else {

            holder.layoutRelated.setVisibility(
                    View.GONE
            );

            holder.txtRelated.setText(
                    ""
            );

            holder.txtRelated.setOnClickListener(
                    null
            );
        }
    }


    // =====================================================
    // SAFE TEXT
    // =====================================================

    private String safeText(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }


    // =====================================================
    // ITEM COUNT
    // =====================================================

    @Override
    public int getItemCount() {

        return messages != null
                ? messages.size()
                : 0;
    }


    // =====================================================
    // WELCOME VIEW HOLDER
    // =====================================================

    private static class WelcomeViewHolder
            extends RecyclerView.ViewHolder {

        private final View suggestionRA9262;
        private final View suggestionTypesOfAbuse;
        private final View suggestionProtectionOrder;
        private final View suggestionFileReport;
        private final View suggestionEmergencyHotlines;
        private final View suggestionSupportSomeone;


        public WelcomeViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);


            suggestionRA9262 =
                    itemView.findViewById(
                            R.id.suggestionRA9262
                    );


            suggestionTypesOfAbuse =
                    itemView.findViewById(
                            R.id.suggestionTypesOfAbuse
                    );


            suggestionProtectionOrder =
                    itemView.findViewById(
                            R.id.suggestionProtectionOrder
                    );


            suggestionFileReport =
                    itemView.findViewById(
                            R.id.suggestionFileReport
                    );


            suggestionEmergencyHotlines =
                    itemView.findViewById(
                            R.id.suggestionEmergencyHotlines
                    );


            suggestionSupportSomeone =
                    itemView.findViewById(
                            R.id.suggestionSupportSomeone
                    );
        }


        // =================================================
        // SUGGESTION ACTIONS
        // =================================================

        private void bindSuggestions(
                OnSuggestionClickListener listener
        ) {

            if (listener == null) {
                return;
            }


            suggestionRA9262.setOnClickListener(
                    v -> listener.onSuggestionClick(
                            "What is RA 9262?"
                    )
            );


            suggestionTypesOfAbuse.setOnClickListener(
                    v -> listener.onSuggestionClick(
                            "What are the types of abuse?"
                    )
            );


            suggestionProtectionOrder.setOnClickListener(
                    v -> listener.onSuggestionClick(
                            "What is a protection order?"
                    )
            );


            suggestionFileReport.setOnClickListener(
                    v -> listener.onSuggestionClick(
                            "How can I report an incident?"
                    )
            );


            suggestionEmergencyHotlines.setOnClickListener(
                    v -> listener.onSuggestionClick(
                            "Where can I find emergency hotlines?"
                    )
            );


            suggestionSupportSomeone.setOnClickListener(
                    v -> listener.onSuggestionClick(
                            "How can I help someone experiencing abuse?"
                    )
            );
        }
    }


    // =====================================================
    // HOPE MESSAGE VIEW HOLDER
    // =====================================================

    private static class HopeMessageViewHolder
            extends RecyclerView.ViewHolder {

        private final TextView txtMessage;

        private final MaterialButton btnAction;

        private final View layoutSource;

        private final TextView txtSource;

        private final View layoutRelated;

        private final TextView txtRelated;


        public HopeMessageViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);


            // =================================================
            // MAIN MESSAGE
            // =================================================

            txtMessage =
                    itemView.findViewById(
                            R.id.txtHopeMessage
                    );


            // =================================================
            // ACTION
            // =================================================

            btnAction =
                    itemView.findViewById(
                            R.id.btnHopeAction
                    );


            // =================================================
            // SOURCE
            // =================================================

            layoutSource =
                    itemView.findViewById(
                            R.id.layoutHopeSource
                    );

            txtSource =
                    itemView.findViewById(
                            R.id.txtHopeSource
                    );


            // =================================================
            // RELATED QUESTIONS
            // =================================================

            layoutRelated =
                    itemView.findViewById(
                            R.id.layoutHopeRelated
                    );

            txtRelated =
                    itemView.findViewById(
                            R.id.txtHopeRelated
                    );
        }
    }


    // =====================================================
    // USER MESSAGE VIEW HOLDER
    // =====================================================

    private static class UserMessageViewHolder
            extends RecyclerView.ViewHolder {

        private final TextView txtMessage;


        public UserMessageViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);


            txtMessage =
                    itemView.findViewById(
                            R.id.txtUserMessage
                    );
        }
    }
}