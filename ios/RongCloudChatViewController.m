#import "RongCloudChatViewController.h"
#import "RongCloudChat.h"
#import <RongIMKit/RongIMKit.h>

@implementation RongCloudChatViewController

- (void)viewDidLoad {
  [super viewDidLoad];
  [self setupCustomCloseButton];
  
  self.displayUserNameInCell = YES;
  
  switch (self.conversationType) {
    case ConversationType_PRIVATE: {
      RCUserInfo *cachedInfo = [[RCIM sharedRCIM] getUserInfoCache:self.targetId];
      self.title = cachedInfo ? cachedInfo.name : @"";
      self.displayUserNameInCell = NO;
      break;
    }
    case ConversationType_GROUP: {
      RCGroup *group = [[RCIM sharedRCIM] getGroupInfoCache:self.targetId];
      self.title = group ? group.groupName : @"群聊";
      break;
    }
    case ConversationType_DISCUSSION: {
      self.title = @"讨论组";
      break;
    }
    default: {
      self.title = self.targetId;
      break;
    }
  }

  UIColor *barColor = [UIColor colorWithRed:244/255.0 green:246/255.0 blue:249/255.0 alpha:1];
  UIView *bottomSafeAreaView = [[UIView alloc] init];
  bottomSafeAreaView.backgroundColor = barColor;
  bottomSafeAreaView.translatesAutoresizingMaskIntoConstraints = NO;
  
  UINavigationBarAppearance *appearance = [[UINavigationBarAppearance alloc] init];
  [appearance configureWithOpaqueBackground];
  appearance.backgroundColor = barColor;
  appearance.shadowColor = [UIColor systemGray5Color];

  self.navigationItem.leftBarButtonItem = nil;
  self.navigationController.navigationBar.standardAppearance = appearance;
  self.navigationController.navigationBar.scrollEdgeAppearance = appearance;
  self.chatSessionInputBarControl.backgroundColor = barColor;
  [self.view addSubview:bottomSafeAreaView];

  [NSLayoutConstraint activateConstraints:@[
    [bottomSafeAreaView.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor],
    [bottomSafeAreaView.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
    [bottomSafeAreaView.topAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.bottomAnchor],
    [bottomSafeAreaView.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor]
  ]];
}

- (void)viewWillDisappear:(BOOL)animated {
  [super viewWillDisappear:animated];
  if (self.isMovingFromParentViewController || self.isBeingDismissed) {
    [RongCloudChat sendEvent:@"onRCIMChatClosed" body:@{ @"conversationType": @((NSInteger)self.conversationType), @"targetId": self.targetId ?: @"" }];
  }
}

- (void)viewDidLayoutSubviews {
  [super viewDidLayoutSubviews];

  CGFloat navBarHeight = self.navigationController.navigationBar.frame.size.height;
  CGFloat buttonSize = 32.0;
  CGFloat safeLeft = self.view.safeAreaInsets.left;
  CGFloat buttonX = safeLeft + 8.0;
  CGFloat buttonY = (navBarHeight - buttonSize) / 2.0;

  self.customCloseButton.frame = CGRectMake(buttonX, buttonY, buttonSize, buttonSize);
}

- (NSArray<UIMenuItem *> *)getLongTouchMessageCellMenuList:(RCMessageModel *)model {
  NSArray<UIMenuItem *> *defaultItems = [super getLongTouchMessageCellMenuList:model];
  NSMutableArray<UIMenuItem *> *filteredItems = [NSMutableArray array];

  for (UIMenuItem *item in defaultItems) {
    NSString *title = item.title ?: @"";
    if ([title containsString:@"更多"] || [title containsString:@"Select"]) {
      continue;
    }
    [filteredItems addObject:item];
  }

  return filteredItems;
}

- (void)onClose {
  [self dismissViewControllerAnimated:YES completion:nil];
}

- (void)setupCustomCloseButton {
  UIImage *closeImage = [UIImage systemImageNamed:@"multiply"];
  self.customCloseButton = [UIButton buttonWithType:UIButtonTypeSystem];
  [self.customCloseButton setImage:closeImage forState:UIControlStateNormal];
  self.customCloseButton.tintColor = [UIColor blackColor];
  self.customCloseButton.backgroundColor = [UIColor clearColor];
  [self.customCloseButton addTarget:self action:@selector(onClose) forControlEvents:UIControlEventTouchUpInside];
  [self.navigationController.navigationBar addSubview:self.customCloseButton];
}

@end
