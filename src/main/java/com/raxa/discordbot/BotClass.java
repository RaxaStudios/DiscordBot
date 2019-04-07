/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.raxa.discordbot;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;
import java.util.Scanner;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.util.logging.ExceptionLogger;
import org.javacord.api.entity.auditlog.*;
import org.javacord.api.entity.message.embed.EmbedBuilder;

/**
 *
 * @author Raxa
 */
public class BotClass {

    private String channel;
    private String user;
    private String displayName;
    private String message;
    private String url;
    private static String text;
    private String mod;
    private static EmbedBuilder embed;
    private String tempId = "";

    public BotClass() {

    }

    String token = "";

    public void start() {
        try {
            //get the token from token.key file
            try{
            Path configFile = Paths.get("token.key");
                Scanner scan = new Scanner(configFile);
                while(scan.hasNext()){
                    token = scan.next();
                }
            } catch(IOException ie){
                System.out.println("could not find token file");
                //ie.printStackTrace();
            }
            new DiscordApiBuilder().setToken(token).login().thenAccept(api -> {

                //set message cache options
                //possible use for attachments or thing
                api.setMessageCacheSize(0, 60 * 60);
                api.setAutomaticMessageCacheCleanupEnabled(true);

                api.addMessageCreateListener(event -> {
                    if (event.getMessageContent().equalsIgnoreCase(("!ping"))) {
                        event.getChannel().sendMessage("Pong");
                    } else {
                        String content = event.getMessageContent();
                        if (content.startsWith("!")) {
                            String send = handleCommand(content);
                            if (!send.equals("")) {
                                event.getChannel().sendMessage(send);
                            }// else ignore
                        }
                    }
                });
                api.addMessageDeleteListener(event -> {
                    try {
                        channel = event.getServerTextChannel().get().getName();
                        if (channel.equals("bot_log")) {
                            //ignore
                        } else {
                            message = event.getMessage().get().getReadableContent();
                            if (event.getMessage().get().getAttachments().size() > 0) {
                                url = event.getMessage().get().getAttachments().get(0).getUrl().toString();
                            } else {
                                url = "";
                            }
                            user = event.getMessageAuthor().get().getDiscriminatedName();
                            displayName = event.getMessageAuthor().get().getDisplayName();
                            mod = "";
                            event.getServer().get().getAuditLog(1, AuditLogActionType.MESSAGE_DELETE).thenAcceptAsync(log -> {
                                if (log.getEntries().size() > 0) {
                                    AuditLogEntry entry = log.getEntries().get(0);
                                    mod = entry.getUser().join().getDiscriminatedName();
                                    try {
                                        System.out.println(entry);
                                        if (!tempId.equals(entry.getIdAsString())) {
                                            tempId = entry.getIdAsString();
                                        } else {
                                            mod = user;
                                        }
                                        /* System.out.println(entry.getTarget().get().asUser().get().getDiscriminatedName());
                                        System.out.println(entry.getUser().get().getDiscriminatedName());
                                        System.out.println(event.getMessage().toString());
                                        System.out.println(event.getMessageAuthor().toString());
                                        System.out.println(event.getMessageContent());
                                        System.out.println(event.getReadableMessageContent());
                                        System.out.println(event.getMessage().get().getActivity());*/
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    String modAction;
                                    modAction = (nowDateFormatted() + ": "
                                            + mod + " deleted "
                                            + user + "'s message: \'"
                                            + message + "\' in channel: #" + channel);
                                    //generate embed and send
                                    event.getServer().get().getTextChannelsByName("bot_log").get(0).sendMessage(createEmbed(channel, mod, user, displayName, message, url));

                                }
                            });
                        }
                    } catch (Exception e) {
                        //ignore
                        e.printStackTrace();
                    }
                });
                api.addServerMemberBanListener(event -> {
                    event.getServer().getAuditLog(1, AuditLogActionType.MEMBER_BAN_ADD).thenAcceptAsync(auditLog -> {
                        if (auditLog.getEntries().size() > 0) {
                            try {
                                AuditLogEntry entry = auditLog.getEntries().get(0);
                                mod = entry.getUser().join().getDiscriminatedName();
                                user = entry.getTarget().get().asUser().get().getDiscriminatedName();
                                String modAction;
                                modAction = (nowDateFormatted() + ": "
                                        + mod + " banned "
                                        + user);
                                event.getServer().getTextChannelsByName("bot_log").get(0).sendMessage(modAction);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                });

                System.out.println("invite link for bot: " + api.createBotInvite());
            }).exceptionally(ExceptionLogger.get());
        } catch (NoSuchElementException nse) {
            System.out.println("failed to get message contents");
            //nse.printStackTrace();
        }
    }

    public static String nowDateFormatted() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String returnDate = dtf.format(now);
        return returnDate;
    }

    public static String handleCommand(String content) {
        text = "That's a command.";
        //TODO handle custom commands
        return text;
    }

    private static EmbedBuilder createEmbed(String channel, String mod, String user, String displayName, String message, String url) {
        embed = new EmbedBuilder()
            .setTitle("Deleted Message")
            .setAuthor(mod)
            .addInlineField("User", user)
            .addInlineField("DisplayName", displayName)
            .addField("Channel", ("#" + channel))
            .setColor(Color.GREEN);

        if (!message.equals("")) {
            embed.addField("Message", "||" + message + "||");
        }

        if (!url.equals("")) {
            embed.addField("Attachment", "||" + url + "||");
        }

        embed.setTimestampToNow();
        return embed;
    }
}
