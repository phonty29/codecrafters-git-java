package executors;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import utils.Git;

public class CommitTree implements Executor{

  private final String[] params;

  public CommitTree(String... params) {
    this.params = params;
  }

  @Override
  public void execute() {
    if (params.length < 6) {
      throw new IllegalArgumentException("Too few arguments for `commit-tree`");
    }
    String treeSha1 = params[1];
    String parentSha1 = params[3];
    String commitMsg = params[5];

    String payload = String.format("""
        tree %s
        parent %s
        author John Doe <john@example.com> 1234567890 +0000
        committer John Doe <john@example.com> 1234567890 +0000
        
        %s
        """, treeSha1, parentSha1, commitMsg);
    String header = String.format("commit %d\0", payload.getBytes(StandardCharsets.UTF_8).length);
    String content = header + payload;
    try {
      byte[] sha1 = Git.createGitObject(content.getBytes(StandardCharsets.UTF_8));
      System.out.println(HexFormat.of().formatHex(sha1));
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }
}
