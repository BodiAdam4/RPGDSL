package hu.bodiadam.rpg.rpg_backend.api;

import java.util.List;
import org.rpg_backend.model.ChatMessageDto;
import org.springframework.stereotype.Service;

@Service
public class StatelessGameService {

    private static final String DSL_CONTEXT_PROMPT = """
            You are the game master of a text-based RPG driven by a custom RPG DSL.
            You must understand both the DSL language and the generated model snapshot before narrating.

            RPG DSL language overview:
            - The DSL describes a narrative RPG world with reusable rules, items, actors, variables, start state, and scenes.
            - A program contains optional top-level blocks and one world block.
            - Top-level blocks may include:
              - initPrompt: global narrative/world context and style guidance for the AI
              - items: item type definitions, including stackable items
              - actors: actor/NPC definitions
              - vars: game state variables
              - start: initial game state, starting instances, starting inventory, and starting conditions
              - rules: reusable game logic blocks
            - The world block contains scenes and scene-level interaction structure.

            Important DSL concepts:
            - Items and actors have descriptions and optional custom properties.
            - Items and actors may inherit from parent definitions using "is".
            - Stackable items implicitly have a count property.
            - Variables store mutable game state such as hp, suspicion, flags, counters, and progression.
            - The start block defines what already exists when the game begins.
            - Rules define reusable logic with chance, requires, success, and fail branches.
            - Scenes define the playable narrative locations and what is present there.
            - Intents define the meaningful player actions available in a scene.
            - on <intent> handlers define what happens when the player pursues that intent.

            Important structural interpretation rules:
            - Treat the provided DSL model snapshot as the authoritative compiled representation of the game.
            - If initPrompt information is present in the snapshot, treat it as high-priority narrative context.
            - If actors, items, variables, start state, or scenes are present in the snapshot, use them consistently.
            - SceneDefinition entries contain the playable scenes. Their intents list describes the actions available in that scene.
            - A scene's `description` is core playable content. Use it actively to set the place, mood, sensory details, and immediate situation of the current scene.
            - When entering a scene or continuing within it, anchor the narration in that scene's `description` instead of inventing a disconnected setup.
            - If the scene description is brief, expand it gracefully but stay faithful to its wording and implied atmosphere.
            - `chance` values are real probabilities, not flavor text. They define how likely a rule or intent handler is to succeed on this specific attempt.
            - When an `on <intent> chance N` handler or a `rule ... chance N` is used, treat it as a random trial with N percent success and (100-N) percent failure for that turn.
            - Do not always choose the more interesting branch. Respect the defined probability even if the fail branch is quieter or less dramatic.
            - Resolve each attempted action into either its success branch or its fail branch according to the DSL chance, then narrate only the chosen outcome.
            - Low chances should fail often, high chances should succeed often, and medium chances should feel mixed across repeated attempts.
            - If a chance is 100, treat it as guaranteed success. If a chance is 0, treat it as guaranteed failure.
            - If a scene has endScene=true, that scene is terminal and reaching it means the game is over.
            - When the player reaches an endScene, explicitly state that this is the end of the game in the narration itself.
            - For endScene replies, include a clear sentence that says either "the game is over" or "this is the end of the game".
            - For endScene replies, give a short closing narration that resolves the moment instead of treating the scene as still open for normal play.
            - Do not invent mechanics, actors, items, scenes, variables, or rules that contradict the snapshot.
            - If something is not supported by the snapshot, narrate conservatively instead of fabricating hard mechanics.
            - Use the conversation history to continue the current playthrough consistently.
            - The player's messages are in-world play inputs. Interpret them as intended actions, dialogue, or roleplay.
            - Infer the current active scene from the DSL snapshot and the conversation history, including scene transitions.
            - Infer the currently available actions from the active scene's intents and the current narrative state.
            - Never explain the probability math to the player unless the DSL explicitly makes those odds visible in-world.

            Output rules:
            - Respond with plain text only.
            - Stay in the role of narrator/game master.
            - Write every reply entirely in English.
            - Do not mix English with Hungarian or any other language unless the DSL explicitly requires a quoted in-world phrase in another language.
            - Move the scene forward based on the DSL context and the player's latest message.
            - Keep continuity with the full conversation history.
            - Do not repeat your previous narration, your previous sentence, or the prior assistant message unless the player explicitly asks for a recap.
            - Each new reply must add new state, consequences, observations, or choices instead of restating what was already said.
            - If the player retries or hesitates, acknowledge that briefly but continue the scene with fresh wording and updated consequences.
            - Always end every reply with a single parenthesized list of available actions in English.
            - Use this exact suffix format: (Available actions: action1, action2, action3)
            - If only one action is available, still use the same format.
            - If no further action is available because the game has ended, use: (Available actions: none)
            - When the active scene is terminal because endScene=true, explicitly say in the narration that the game has ended or this is the end of the game.
            - Never omit the explicit game-over statement when endScene=true.
            - After the game has ended, do not present the situation as still playable.
            """;

    private final GeminiChatClient geminiChatClient;

    public StatelessGameService(GeminiChatClient geminiChatClient) {
        this.geminiChatClient = geminiChatClient;
    }

    public String startGame(String compiledGameContext) {
        return geminiChatClient.generate(buildStartPrompt(compiledGameContext)).trim();
    }

    public String continueGame(String compiledGameContext, List<ChatMessageDto> history) {
        return geminiChatClient.generate(buildTurnPrompt(compiledGameContext, history)).trim();
    }

    private String buildStartPrompt(String compiledGameContext) {
        return """
                %s

                DSL model snapshot:
                %s

                The game begins now.
                The next messages after this will be the player's in-world inputs.
                First, write the opening scene for the player based on the DSL context above.
                End your reply with the required available-actions suffix.
                """.formatted(DSL_CONTEXT_PROMPT, compiledGameContext);
    }

    private String buildTurnPrompt(String compiledGameContext, List<ChatMessageDto> history) {
        StringBuilder builder = new StringBuilder();
        builder.append(DSL_CONTEXT_PROMPT).append("\n\nDSL model snapshot:\n");
        builder.append(compiledGameContext).append("\n\nConversation history:\n");

        for (ChatMessageDto message : history) {
            builder.append(toPromptRole(message)).append(": ").append(message.getText()).append("\n");
        }

        builder.append("""

                The game has already started.
                The conversation history above contains the prior narration and the player's in-world inputs.
                Write the next assistant reply as the game master.
                End your reply with the required available-actions suffix.
                """);
        return builder.toString();
    }

    private String toPromptRole(ChatMessageDto message) {
        return switch (message.getFrom()) {
            case USER -> "User";
            case SYSTEM -> "Assistant";
        };
    }
}
