package com.hestia.vault.ai;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Dedicated Time & Environmental Grounding Service for Hestia 2.0.
 * Eliminates server-container timezone drift and provides accurate real-time
 * awareness calibrated to the user's local timezone (default: Asia/Kolkata).
 */
public class HestiaTimeService {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Kolkata");

    public static ZoneId resolveZone(String timeZoneStr) {
        if (timeZoneStr != null && !timeZoneStr.isBlank()) {
            try {
                return ZoneId.of(timeZoneStr.trim());
            } catch (Exception ignored) {}
        }
        return DEFAULT_ZONE;
    }

    public static ZonedDateTime now(String timeZoneStr) {
        return ZonedDateTime.now(resolveZone(timeZoneStr));
    }

    public static String formatTime(ZonedDateTime zdt) {
        return zdt.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH));
    }

    public static String formatDate(ZonedDateTime zdt) {
        return zdt.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH));
    }

    public static String getTimeOfDayPeriod(int hour) {
        if (hour >= 5 && hour < 12) {
            return "Morning";
        } else if (hour >= 12 && hour < 17) {
            return "Afternoon";
        } else if (hour >= 17 && hour < 22) {
            return "Evening";
        } else {
            return "Late Night";
        }
    }

    public static boolean isTimeQuery(String text) {
        if (text == null) return false;
        String l = text.trim().toLowerCase();
        return l.matches(".*\\b(what\\s+time\\s+is\\s+it|what(?:'s|s|\\s+is)\\s+the\\s+time|current\\s+time|time\\s+check|tell\\s+me\\s+the\\s+time|what\\s+is\\s+your\\s+time|what\\s+time\\s+now|time\\s+now|exact\\s+time|what's\\s+the\\s+current\\s+time|whats\\s+the\\s+time|what\\s+time\\s+do\\s+you\\s+have)\\b.*")
            || l.matches("^\\s*time\\s*\\??\\s*$")
            || l.matches(".*\\b(do\\s+you\\s+know\\s+what\\s+time\\s+it\\s+is|do\\s+you\\s+know\\s+the\\s+time|can\\s+you\\s+tell\\s+me\\s+the\\s+time)\\b.*");
    }

    public static HestiaNeuralSimulator.SimulationResult buildTimeResponse(String query, String timeZoneStr, boolean isCreator, String username) {
        ZonedDateTime zdt = now(timeZoneStr);
        String timeStr = formatTime(zdt);
        String dateStr = formatDate(zdt);
        String zoneId = zdt.getZone().getId();
        int hour = zdt.getHour();
        String period = getTimeOfDayPeriod(hour);

        StringBuilder sb = new StringBuilder();
        sb.append("It's currently **").append(timeStr).append("** (").append(zoneId).append("), ").append(dateStr).append(".\n\n");

        if (isCreator) {
            if (hour >= 23 || hour < 5) {
                sb.append("It's the middle of the night, Nichu (").append(timeStr).append("). Go to sleep or I'm revoking your git push access before you break main.");
            } else if (hour < 12) {
                sb.append("Fresh morning hours, Nichu. Ready to ship something clean, or are you still staring at yesterday's terminal?");
            } else if (hour < 17) {
                sb.append("Broad daylight in the middle of the afternoon, Nichu. Crisp, productive hours—what are we building right now?");
            } else {
                sb.append("Evening hours, Nichu. Wrapping up today's feature branches or reviewing vault logs?");
            }
        } else {
            if (hour >= 23 || hour < 5) {
                sb.append("It's ").append(timeStr).append(" in the dead of the night. Burning the midnight oil, ").append(username).append("? Don't forget to push your code before you pass out.");
            } else if (hour < 12) {
                sb.append("Good morning, ").append(username).append("! What are we checking today—academic vault, job scanner, or career prep?");
            } else if (hour < 17) {
                sb.append("Good afternoon, ").append(username).append("! What are we tackling in Hestia right now?");
            } else {
                sb.append("Good evening, ").append(username).append("! Ready to scan today's job radar or drill on interview questions?");
            }
        }

        HestiaPersonaConfig.MoodState mood = (hour >= 23 || hour < 5) ? HestiaPersonaConfig.MoodState.DEEP_CONFIDANTE : 
            (isCreator ? HestiaPersonaConfig.MoodState.CREATOR_BOND : HestiaPersonaConfig.MoodState.CHILL_LOUNGE);
        List<String> actions = List.of("Audit my academic vault", "Check job radar", "Review tech stack");
        return new HestiaNeuralSimulator.SimulationResult(sb.toString(), mood, actions, 18);
    }

    public static String buildTimeResponseString(String query, String timeZoneStr, boolean isCreator, String username) {
        return buildTimeResponse(query, timeZoneStr, isCreator, username).getReply();
    }
}
