package com.example.goalbot.agent.plan;

import com.example.goalbot.agent.ToolNames;
import com.example.goalbot.dto.command.CommandIntent;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AgentPlanIntentAdapter {

    public CommandIntent toIntent(AgentPlan plan) {
        CommandIntent intent = CommandIntent.of(toIntentType(plan == null ? null : plan.primaryTool()));
        intent.setSource(plan == null ? "agent-plan" : plan.getSource());
        intent.setConfidence(plan == null ? 0.0 : plan.getConfidence());
        intent.setRequiresConfirmation(plan != null && Boolean.TRUE.equals(plan.getRequiresConfirmation()));
        intent.setClarifyingQuestion(plan == null ? null : plan.getClarifyingQuestion());
        intent.setAssistantReply(plan == null ? null : plan.getAssistantReply());
        if (plan == null || plan.getActions() == null || plan.getActions().isEmpty()) {
            return intent;
        }

        AgentAction action = plan.getActions().get(0);
        Map<String, Object> args = action.getArguments();
        intent.setTaskTitle(text(args, "task_title", "title"));
        intent.setTaskKeyword(text(args, "task_keyword", "task_query"));
        intent.setDescription(text(args, "description"));
        intent.setPlanDate(text(args, "plan_date"));
        intent.setRangeStartDate(text(args, "range_start_date"));
        intent.setRangeEndDate(text(args, "range_end_date"));
        intent.setStartTime(text(args, "start_time"));
        intent.setEndTime(text(args, "end_time"));
        intent.setPlannedMinutes(integer(args, "planned_minutes"));
        intent.setActualMinutes(integer(args, "actual_minutes"));
        intent.setGoalId(longValue(args, "goal_id"));
        intent.setGoalKeyword(text(args, "goal_keyword"));
        intent.setMissingSlots(action.getMissingSlots());
        return intent;
    }

    private CommandIntent.Intent toIntentType(String tool) {
        if (tool == null) {
            return CommandIntent.Intent.UNKNOWN;
        }
        return switch (tool) {
            case ToolNames.LIST_TODAY_TASKS -> CommandIntent.Intent.TODAY_TASKS;
            case ToolNames.LIST_TASKS_BY_DATE -> CommandIntent.Intent.LIST_TASKS_BY_DATE;
            case ToolNames.CREATE_TASK -> CommandIntent.Intent.CREATE_TASK;
            case ToolNames.UPDATE_TASK_DRAFT -> CommandIntent.Intent.CREATE_TASK;
            case ToolNames.UPDATE_TASK_SCHEDULE -> CommandIntent.Intent.UPDATE_TASK_SCHEDULE;
            case ToolNames.CANCEL_TASKS -> CommandIntent.Intent.CANCEL_TASKS;
            case ToolNames.CANCEL_IMPORTED_SCHEDULE -> CommandIntent.Intent.CANCEL_IMPORTED_SCHEDULE;
            case ToolNames.CHECKIN_TASK -> CommandIntent.Intent.CHECKIN;
            case ToolNames.GOAL_STATUS -> CommandIntent.Intent.GOAL_STATUS;
            case ToolNames.GENERATE_ADVICE -> CommandIntent.Intent.ADVICE;
            case ToolNames.DAILY_REVIEW -> CommandIntent.Intent.DAILY_REVIEW;
            case ToolNames.WEEKLY_REVIEW -> CommandIntent.Intent.WEEKLY_REVIEW;
            case ToolNames.HELP -> CommandIntent.Intent.HELP;
            default -> CommandIntent.Intent.UNKNOWN;
        };
    }

    private String text(Map<String, Object> args, String... keys) {
        Object value = first(args, keys);
        return value == null ? null : value.toString().trim();
    }

    private Integer integer(Map<String, Object> args, String key) {
        Object value = first(args, key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? null : Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long longValue(Map<String, Object> args, String key) {
        Object value = first(args, key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? null : Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Object first(Map<String, Object> args, String... keys) {
        if (args == null) {
            return null;
        }
        for (String key : keys) {
            if (args.get(key) != null) {
                return args.get(key);
            }
        }
        return null;
    }
}
