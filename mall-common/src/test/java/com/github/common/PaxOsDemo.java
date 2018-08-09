package com.github.common;

import java.util.*;

public final class PaxOsDemo {

    private static final Random RANDOM = new Random();
    private static final String[] PROPOSALS = {"ProjectA", "ProjectB", "ProjectC"};

    public static void main(String[] args) {
        List<Acceptor> acceptors = new ArrayList<>();
        for (String name : Arrays.asList("A", "B", "C", "D", "E")) {
            acceptors.add(new Acceptor(name));
        }
        Proposer.vote(new Proposal(1L, null), acceptors);
    }

    private static void printInfo(String subject, String operation, String result) {
        System.out.println(subject + ":" + operation + "<" + result + ">");
    }

    /**
     * 对于提案的约束，第三条约束要求：
     * 如果 maxVote 不存在，那么没有限制，下一次表决可以使用任意提案；
     * 否则，下一次表决要沿用 maxVote 的提案
     */
    private static Proposal nextProposal(long currentVoteNumber, List<Proposal> proposals) {
        long voteNumber = currentVoteNumber + 1;
        if (proposals.isEmpty()) {
            return new Proposal(voteNumber, PROPOSALS[RANDOM.nextInt(PROPOSALS.length)]);
        }
        Collections.sort(proposals);

        Proposal maxVote = proposals.get(proposals.size() - 1);
        long maxVoteNumber = maxVote.getVoteNumber();
        if (maxVoteNumber >= currentVoteNumber)
            throw new IllegalStateException("illegal state maxVoteNumber");

        String content = maxVote.getContent();
        if (content != null) {
            return new Proposal(voteNumber, content);
        } else {
            return new Proposal(voteNumber, PROPOSALS[RANDOM.nextInt(PROPOSALS.length)]);
        }
    }

    private static class Proposer {
        static void vote(Proposal proposal, Collection<Acceptor> acceptors) {
            int quorum = Math.floorDiv(acceptors.size(), 2) + 1;
            int count = 0;
            while (true) {
                count++;
                printInfo("\n投票回合", "开始轮数",  String.valueOf(count));
                List<Proposal> proposals = new ArrayList<>();
                for (Acceptor acceptor : acceptors) {
                    Promise promise = acceptor.onPrepare(proposal);
                    if (promise != null && promise.isAck())
                        proposals.add(promise.getProposal());
                }
                if (proposals.size() < quorum) {
                    printInfo("提议者[" + proposal + "]", "投票时", "还没准备好");
                    proposal = nextProposal(proposal.getVoteNumber(), proposals);
                    continue;
                }
                int acceptCount = 0;
                for (Acceptor acceptor : acceptors) {
                    if (acceptor.onAccept(proposal))
                        acceptCount++;
                }
                if (acceptCount < quorum) {
                    printInfo("提议者[" + proposal + "]", "投票时", "不接受");
                    proposal = nextProposal(proposal.getVoteNumber(), proposals);
                    continue;
                }
                break;
            }
            printInfo("提议者[" + proposal + "]", "投票时", "通过");
        }
    }

    private static class Acceptor {

        //上次表决结果
        private Proposal last = new Proposal();
        private String name;

        Acceptor(String name) {
            this.name = name;
        }

        Promise onPrepare(Proposal proposal) {
            //假设这个过程有50%的几率失败
            if (Math.random() - 0.5 > 0) {
                printInfo("接收者_" + name, "准备", "没有反应");
                return null;
            }
            if (proposal == null)
                throw new IllegalArgumentException("null proposal");
            if (proposal.getVoteNumber() > last.getVoteNumber()) {
                Promise response = new Promise(true, last);
                last = proposal;
                printInfo("接收者_" + name, "准备", "OK");
                return response;
            } else {
                printInfo("接收者_" + name, "准备", "拒绝");
                return new Promise(false, null);
            }
        }
        boolean onAccept(Proposal proposal) {
            //假设这个过程有50%的几率失败
            if (Math.random() - 0.5 > 0) {
                printInfo("接收者_" + name, "接受", "没有反应");
                return false;
            }
            printInfo("接收者_" + name, "接受", "OK");
            return last.equals(proposal);
        }
    }

    private static class Promise {

        private final boolean ack;
        private final Proposal proposal;

        Promise(boolean ack, Proposal proposal) {
            this.ack = ack;
            this.proposal = proposal;
        }

        boolean isAck() {
            return ack;
        }

        Proposal getProposal() {
            return proposal;
        }
    }

    private static class Proposal implements Comparable<Proposal> {

        private final long voteNumber;
        private final String content;

        Proposal(long voteNumber, String content) {
            this.voteNumber = voteNumber;
            this.content = content;
        }

        Proposal() {
            this(0, null);
        }

        long getVoteNumber() {
            return voteNumber;
        }

        String getContent() {
            return content;
        }

        @Override
        public int compareTo(Proposal o) {
            return Long.compare(voteNumber, o.voteNumber);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof Proposal)) return false;

            Proposal proposal = (Proposal) obj;
            return voteNumber == proposal.voteNumber && Objects.equals(content, proposal.content);
        }

        @Override
        public String toString() {
            return voteNumber + " : " + content;
        }
    }
}
